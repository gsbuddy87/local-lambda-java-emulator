variable "environment" {
  description = "The deployment environment (local or aws)"
  type        = string
  default     = "local"
}

variable "aws_region" {
  description = "AWS Region"
  type        = string
  default     = "us-east-1"
}

variable "s3_bucket_name" {
  description = "Name of the S3 bucket"
  type        = string
  default     = "my-comparison-bucket-emulator"
}
