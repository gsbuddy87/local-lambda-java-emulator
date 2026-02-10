# --- S3 Bucket ---
resource "aws_s3_bucket" "comparison_bucket" {
  bucket = var.s3_bucket_name
}

# --- IAM Role for Lambda ---
resource "aws_iam_role" "lambda_role" {
  count = var.environment == "aws" ? 1 : 0
  name  = "s3_comparator_lambda_role_${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "lambda.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy" "lambda_policy" {
  count = var.environment == "aws" ? 1 : 0
  name  = "s3_comparator_lambda_policy_${var.environment}"
  role  = aws_iam_role.lambda_role[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Effect   = "Allow"
        Resource = "arn:aws:logs:*:*:*"
      },
      {
        Action = ["s3:GetObject", "s3:ListBucket"]
        Effect = "Allow"
        Resource = [
          aws_s3_bucket.comparison_bucket.arn,
          "${aws_s3_bucket.comparison_bucket.arn}/*"
        ]
      },
      # Allow connection to RDS if in AWS
      {
        Action = [
            "ec2:CreateNetworkInterface",
            "ec2:DescribeNetworkInterfaces",
            "ec2:DeleteNetworkInterface"
        ]
        Effect = "Allow"
        Resource = "*"
      }
    ]
  })
}

# --- Security Groups (AWS Only) ---
resource "aws_security_group" "lambda_sg" {
  count       = var.environment == "aws" ? 1 : 0
  name        = "comparator-lambda-sg"
  description = "Security group for Lambda"
  vpc_id      = data.aws_vpc.default[0].id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "rds_sg" {
  count       = var.environment == "aws" ? 1 : 0
  name        = "comparator-rds-sg"
  description = "Security group for RDS"
  vpc_id      = data.aws_vpc.default[0].id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.lambda_sg[0].id]
  }
}

# --- Aurora Serverless v2 (AWS Only) ---
resource "aws_db_subnet_group" "aurora_subnet_group" {
  count      = var.environment == "aws" ? 1 : 0
  name       = "comparator-db-subnet-group"
  subnet_ids = data.aws_subnets.default[0].ids
}

resource "aws_rds_cluster" "aurora_cluster" {
  count                   = var.environment == "aws" ? 1 : 0
  cluster_identifier      = "comparator-cluster"
  engine                  = "aurora-postgresql"
  engine_mode             = "provisioned"
  engine_version          = "15.4"
  database_name           = "results"
  master_username         = "postgres"
  master_password         = "postgrespassword123" # Simple for demo
  skip_final_snapshot     = true
  vpc_security_group_ids  = [aws_security_group.rds_sg[0].id]
  db_subnet_group_name    = aws_db_subnet_group.aurora_subnet_group[0].name

  serverlessv2_scaling_configuration {
    max_capacity = 1.0
    min_capacity = 0.5
  }
}

resource "aws_rds_cluster_instance" "aurora_instance" {
  count              = var.environment == "aws" ? 1 : 0
  identifier         = "comparator-instance"
  cluster_identifier = aws_rds_cluster.aurora_cluster[0].id
  instance_class     = "db.serverless"
  engine             = aws_rds_cluster.aurora_cluster[0].engine
  engine_version     = aws_rds_cluster.aurora_cluster[0].engine_version
}

# --- Lambda Function ---
resource "aws_lambda_function" "comparator_lambda" {
  filename         = "../target/s3-comparator-lambda-1.0-SNAPSHOT.jar"
  function_name    = "s3-comparator"
  role             = var.environment == "aws" ? aws_iam_role.lambda_role[0].arn : "arn:aws:iam::000000000000:role/lambda-role"
  handler          = "com.emulator.lambda.Handler::handleRequest"
  source_code_hash = fileexists("../target/s3-comparator-lambda-1.0-SNAPSHOT.jar") ? filebase64sha256("../target/s3-comparator-lambda-1.0-SNAPSHOT.jar") : null
  runtime          = "java25"
  timeout          = 300 # 5 minutes for large files
  memory_size      = 512

  environment {
    variables = {
      S3_ENDPOINT          = var.environment == "local" ? "http://localstack-main:4566" : "" # Use internal container name for peer-to-peer networking
      DB_CONNECTION_STRING = var.environment == "aws" ? "jdbc:postgresql://${aws_rds_cluster.aurora_cluster[0].endpoint}:5432/results?user=postgres&password=postgrespassword123" : "jdbc:sqlite:/tmp/results.db"
    }
  }

  # Config for AWS (VPC)
  dynamic "vpc_config" {
    for_each = var.environment == "aws" ? [1] : []
    content {
      subnet_ids         = data.aws_subnets.default[0].ids
      security_group_ids = [aws_security_group.lambda_sg[0].id]
    }
  }
}
