variable "project_name" {
  description = "Project name prefix"
}

variable "private_subnet_ids" {
  description = "Private subnet IDs from networking module"
  type        = list(string)
}

variable "rds_sg_id" {
  description = "RDS security group ID from security module"
}

variable "redis_sg_id" {
  description = "Redis security group ID from security module"
}

variable "db_username" {
  description = "RDS master username"
}

variable "db_password" {
  description = "RDS master password"
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT secret key"
  sensitive   = true
}
