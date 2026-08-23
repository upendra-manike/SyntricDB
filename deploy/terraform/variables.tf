variable "aws_region" {
  description = "AWS Region to deploy SyntricDB into"
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "EC2 instance size"
  type        = string
  default     = "t3.medium"
}

variable "admin_password" {
  description = "SyntricDB Admin Password"
  type        = string
  default     = "syntricdb_secret_pass"
  sensitive   = true
}

variable "key_name" {
  description = "Optional SSH key pair name for EC2 access"
  type        = string
  default     = ""
}
