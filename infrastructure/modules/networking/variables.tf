variable "project_name" {
  description = "Project name prefix"
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
}

variable "public_subnet_a_cidr" {
  description = "Public subnet A CIDR"
}

variable "public_subnet_b_cidr" {
  description = "Public subnet B CIDR"
}

variable "private_subnet_a_cidr" {
  description = "Private subnet A CIDR"
}

variable "private_subnet_b_cidr" {
  description = "Private subnet B CIDR"
}

variable "azs" {
  description = "Availability zones"
  type        = list(string)
}

variable "cluster_name" {
  description = "EKS cluster name — Karpenter/ALB discovery 태그에 사용"
  type        = string
}
