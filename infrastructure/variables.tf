############################################
# AWS 기본 설정
############################################

variable "aws_profile" {
  description = "AWS CLI profile name"
  default     = "goorm"
}

variable "region" {
  description = "AWS region"
  default     = "ap-southeast-2"
}

variable "project_name" {
  description = "Project name prefix for resource naming"
  default     = "pposiraegi"
}

############################################
# VPC 네트워크 설정
############################################

variable "vpc_cidr" {
  description = "VPC CIDR block"
  default     = "10.0.0.0/16"
}

############################################
# Public Subnet 설정
############################################
variable "public_subnet_a_cidr" {
  description = "Public subnet A CIDR (AZ-a)"
  default     = "10.0.1.0/24"
}

variable "public_subnet_b_cidr" {
  description = "Public subnet B CIDR (AZ-b)"
  default     = "10.0.2.0/24"
}

############################################
# Private Subnet 설정
############################################

variable "private_subnet_a_cidr" {
  description = "Private subnet A CIDR (RDS / ElastiCache용)"
  default     = "10.0.11.0/24"
}

variable "private_subnet_b_cidr" {
  description = "Private subnet B CIDR (RDS / ElastiCache Multi-AZ용)"
  default     = "10.0.12.0/24"
}

############################################
# EC2 설정
############################################

variable "ec2_ami" {
  description = "AMI for EC2 (Amazon Linux 2023, ap-southeast-2)"
  default     = "ami-0c9c942bd7bf113a2"  # Amazon Linux 2023
}

variable "ec2_instance_type" {
  description = "EC2 instance type"
  default     = "t3.medium"
}

############################################
# SSH / Key 설정
############################################

variable "ssh_public_key_path" {
  description = "Path to SSH public key"
  default     = "~/.ssh/id_ed25519.pub"
}

############################################
# 접근 제어
############################################

variable "my_ip" {
  description = "Your public IP for SSH access (CIDR 형식)"
}

############################################
# 애플리케이션 설정
############################################

variable "github_repo" {
  description = "GitHub repo URL to clone on EC2"
  default     = "https://github.com/Goorm4I/pposiraegi-ecommerce.git"
}

variable "jwt_secret" {
  description = "JWT secret key for Spring Boot"
  sensitive   = true
}

############################################
# RDS 설정
############################################

variable "db_username" {
  description = "RDS master username"
  default     = "pposiraegi"
}

variable "db_password" {
  description = "RDS master password"
  sensitive   = true
}
