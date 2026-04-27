############################################
# AWS 기본 설정
############################################

variable "aws_profile" {
  description = "AWS CLI profile name"
  default     = "goorm"
}

variable "region" {
  description = "AWS region"
  default     = "ap-northeast-2"
}

variable "github_repo" {
  description = "GitHub repository name (format: owner/repo)"
  default     = "Goorm4I/pposiraegi-ecommerce-msa"
}

variable "github_branch" {
  description = "GitHub branch to trigger the pipeline"
  default     = "main"
}

variable "codestar_connection_arn" {
  description = "CodeStar Connection ARN for GitHub"
  default     = "arn:aws:codeconnections:ap-northeast-2:779846782353:connection/f2153b0a-6274-4ffa-b82c-f90f2cf52c0d"
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
# 애플리케이션 설정
############################################

variable "domain_name" {
  description = "Custom domain for Route53 and ACM"
  default     = "pposiraegi.cloud"
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

############################################
# EKS 설정
############################################

variable "eks_cluster_version" {
  description = "EKS 클러스터 버전"
  default     = "1.29"
}

variable "eks_node_instance_type" {
  description = "EKS 워커 노드 인스턴스 타입"
  default     = "t3.medium"
}

variable "eks_node_desired_size" {
  description = "EKS 워커 노드 기본 수"
  default     = 2
}

variable "eks_node_min_size" {
  description = "EKS 워커 노드 최소 수"
  default     = 1
}

variable "eks_node_max_size" {
  description = "EKS 워커 노드 최대 수"
  default     = 5
}