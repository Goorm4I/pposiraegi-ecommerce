variable "project_name" {
  description = "Project name prefix"
}

variable "vpc_id" {
  description = "VPC ID from networking module"
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for EKS nodes"
  type        = list(string)
}

variable "public_subnet_ids" {
  description = "Public subnet IDs for ALB"
  type        = list(string)
}

variable "cluster_version" {
  description = "EKS cluster version"
}

variable "node_instance_type" {
  description = "EKS worker node instance type"
}

variable "node_desired_size" {
  description = "EKS worker node desired count"
}

variable "node_min_size" {
  description = "EKS worker node min count"
}

variable "node_max_size" {
  description = "EKS worker node max count"
}