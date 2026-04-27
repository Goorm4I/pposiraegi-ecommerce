output "cluster_name" {
  description = "EKS 클러스터 이름"
  value       = module.eks.cluster_name
}

output "cluster_endpoint" {
  description = "EKS API Server 엔드포인트"
  value       = module.eks.cluster_endpoint
}

output "kubeconfig_command" {
  description = "kubeconfig 업데이트 명령어"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

output "karpenter_controller_role_arn" {
  description = "Karpenter Controller IAM Role ARN"
  value       = module.karpenter.controller_role_arn
}

output "lbc_role_arn" {
  description = "AWS Load Balancer Controller IAM Role ARN"
  value       = module.irsa.lbc_role_arn
}

output "alb_security_group_id" {
  description = "ALB SG ID (Ingress annotation에 주입)"
  value       = module.alb.alb_security_group_id
}

output "rds_endpoint" {
  description = "RDS 엔드포인트"
  value       = module.rds.db_endpoint
}

output "redis_endpoint" {
  description = "Redis Primary 엔드포인트"
  value       = module.elasticache.redis_primary_endpoint
}

output "service_role_arns" {
  description = "서비스별 IRSA Role ARN"
  value       = module.irsa.service_role_arns
}
