variable "project_name" {
  description = "Project name prefix"
}

variable "vpc_id" {
  description = "VPC ID from networking module"
}

variable "eks_node_sg_id" {
  description = "Security group ID for EKS/Karpenter worker nodes"
  type        = string
}
