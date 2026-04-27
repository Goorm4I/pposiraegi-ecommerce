variable "cluster_name" {
  description = "EKS 클러스터 이름"
  type        = string
}

variable "kubernetes_version" {
  description = "Kubernetes 버전"
  type        = string
  default     = "1.31"
}

variable "public_subnet_ids" {
  description = "퍼블릭 서브넷 ID 목록"
  type        = list(string)
}

variable "private_subnet_ids" {
  description = "프라이빗 서브넷 ID 목록 (노드그룹 배치)"
  type        = list(string)
}

variable "public_access_cidrs" {
  description = "EKS API Server 퍼블릭 접근 허용 CIDR 목록"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}
