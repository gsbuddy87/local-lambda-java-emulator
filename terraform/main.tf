terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.20.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  # LocalStack configurations
  access_key                  = var.environment == "local" ? "test" : null
  secret_key                  = var.environment == "local" ? "test" : null
  skip_credentials_validation = var.environment == "local"
  skip_metadata_api_check     = var.environment == "local"
  skip_requesting_account_id  = var.environment == "local"
  s3_use_path_style           = var.environment == "local"

  dynamic "endpoints" {
    for_each = var.environment == "local" ? [1] : []
    content {
      s3       = "http://localhost:4567"
      lambda   = "http://localhost:4567"
      iam      = "http://localhost:4567"
      rds      = "http://localhost:4567"
    }
  }
}
