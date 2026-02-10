# Only fetch VPC details if we are in AWS environment (or if needed for local networking)
data "aws_vpc" "default" {
  count   = var.environment == "aws" ? 1 : 0
  default = true
}

data "aws_subnets" "default" {
  count = var.environment == "aws" ? 1 : 0
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default[0].id]
  }
}
