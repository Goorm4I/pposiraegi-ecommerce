variable "project_name" {
  description = "프로젝트 이름"
  type        = string
}

variable "cluster_name" {
  description = "EKS 클러스터 이름 (karpenter.sh/discovery 태그에 사용)"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR 블록 (EKS 노드 내부 통신 허용)"
  type        = string
}
