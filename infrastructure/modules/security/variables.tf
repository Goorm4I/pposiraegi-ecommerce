variable "project_name" {
  description = "Project name prefix"
}

variable "vpc_id" {
  description = "VPC ID from networking module"
}

variable "eks_cluster_sg_id" {
  description = "EKS cluster security group ID"
  default     = ""
}