output "cluster_name" {
  description = "EKS 클러스터 이름"
  value       = aws_eks_cluster.main.name
}

output "cluster_endpoint" {
  description = "EKS 클러스터 API 엔드포인트"
  value       = aws_eks_cluster.main.endpoint
}

output "cluster_certificate_authority" {
  description = "EKS 클러스터 인증서"
  value       = aws_eks_cluster.main.certificate_authority[0].data
}

output "cluster_security_group_id" {
  description = "EKS 클러스터 보안 그룹 ID"
  value       = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
}

output "node_group_arn" {
  description = "EKS 노드 그룹 ARN"
  value       = aws_eks_node_group.main.arn
}
<<<<<<< HEAD

output "node_role_arn" {
  description = "EKS 노드 IAM Role ARN — Karpenter instance profile에서 사용"
  value       = aws_iam_role.eks_node_role.arn
}

output "oidc_provider_arn" {
  description = "OIDC Provider ARN — IRSA trust policy에서 사용"
  value       = aws_iam_openid_connect_provider.eks.arn
}

output "oidc_provider_url" {
  description = "OIDC Provider URL (https:// 제외) — IRSA condition에서 사용"
  value       = replace(aws_iam_openid_connect_provider.eks.url, "https://", "")
}
=======
>>>>>>> origin/feat/eks-migration
