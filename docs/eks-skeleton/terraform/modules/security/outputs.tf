output "eks_node_security_group_id" {
  description = "EKS 노드 추가 SG ID (Karpenter EC2NodeClass에서 참조)"
  value       = aws_security_group.eks_node.id
}

output "rds_security_group_id" {
  description = "RDS Security Group ID"
  value       = aws_security_group.rds.id
}

output "redis_security_group_id" {
  description = "Redis Security Group ID"
  value       = aws_security_group.redis.id
}

output "security_group_ids" {
  description = "모든 Security Group ID 맵"
  value = {
    eks_node = aws_security_group.eks_node.id
    rds      = aws_security_group.rds.id
    redis    = aws_security_group.redis.id
  }
}
