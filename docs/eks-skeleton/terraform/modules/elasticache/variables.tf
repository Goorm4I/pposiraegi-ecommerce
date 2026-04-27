variable "project_name" {
  description = "프로젝트 이름"
  type        = string
}

variable "subnet_ids" {
  description = "ElastiCache 서브넷 ID 목록 (프라이빗 서브넷)"
  type        = list(string)
}

variable "security_group_id" {
  description = "Redis Security Group ID"
  type        = string
}

variable "node_type" {
  description = "ElastiCache 노드 타입"
  type        = string
  default     = "cache.t3.micro"
}

variable "num_cache_clusters" {
  description = "캐시 클러스터 수 (1=단일, 2=HA)"
  type        = number
  default     = 1
}
