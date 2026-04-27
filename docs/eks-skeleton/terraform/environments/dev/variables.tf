variable "aws_region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "프로젝트 이름"
  type        = string
  default     = "pposiraegi"
}

variable "cluster_name" {
  description = "EKS 클러스터 이름"
  type        = string
  default     = "pposiraegi-eks"
}

variable "vpc_cidr" {
  description = "VPC CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}

variable "db_password" {
  description = "RDS 관리자 비밀번호"
  type        = string
  sensitive   = true
}
