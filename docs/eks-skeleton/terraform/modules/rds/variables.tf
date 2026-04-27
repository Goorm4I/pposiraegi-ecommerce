variable "project_name" {
  description = "프로젝트 이름"
  type        = string
}

variable "subnet_ids" {
  description = "RDS 서브넷 ID 목록 (프라이빗 서브넷)"
  type        = list(string)
}

variable "security_group_id" {
  description = "RDS Security Group ID"
  type        = string
}

variable "instance_class" {
  description = "RDS 인스턴스 타입"
  type        = string
  default     = "db.t3.micro"
}

variable "allocated_storage" {
  description = "초기 스토리지 크기 (GB)"
  type        = number
  default     = 20
}

variable "max_allocated_storage" {
  description = "최대 스토리지 크기 (GB, autoscaling)"
  type        = number
  default     = 100
}

variable "db_name" {
  description = "데이터베이스 이름"
  type        = string
  default     = "pposiraegi"
}

variable "db_username" {
  description = "DB 관리자 사용자명"
  type        = string
  default     = "postgres"
}

variable "db_password" {
  description = "DB 관리자 비밀번호"
  type        = string
  sensitive   = true
}

variable "multi_az" {
  description = "Multi-AZ 활성화 여부"
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "삭제 보호 활성화 여부"
  type        = bool
  default     = false
}
