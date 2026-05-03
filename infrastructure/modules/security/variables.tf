variable "project_name" {
  description = "Project name prefix"
}

variable "vpc_id" {
  description = "VPC ID from networking module"
}

<<<<<<< HEAD
variable "eks_node_sg_id" {
  description = "Security group ID for EKS/Karpenter worker nodes"
  type        = string
}
=======
variable "eks_cluster_sg_id" {
  description = "EKS cluster security group ID"
  default     = ""
}
>>>>>>> origin/feat/eks-migration
