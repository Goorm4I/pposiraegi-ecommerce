variable "project_name" {
  description = "프로젝트 이름 (태그 및 리소스 네이밍에 사용)"
  type        = string
}

variable "cluster_name" {
  description = "EKS 클러스터 이름 (서브넷 태그 및 Karpenter discovery에 사용)"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnets" {
  description = "퍼블릭 서브넷 구성 (ALB용)"
  type = map(object({
    cidr = string
    az   = string
  }))
  default = {
    a = { cidr = "10.0.1.0/24", az = "ap-northeast-2a" }
    b = { cidr = "10.0.2.0/24", az = "ap-northeast-2c" }
  }
}

variable "private_subnets" {
  description = "프라이빗 서브넷 구성 (EKS 노드, RDS, ElastiCache용)"
  type = map(object({
    cidr = string
    az   = string
  }))
  default = {
    a = { cidr = "10.0.3.0/24", az = "ap-northeast-2a" }
    b = { cidr = "10.0.4.0/24", az = "ap-northeast-2c" }
  }
}
