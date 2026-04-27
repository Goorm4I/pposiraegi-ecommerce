output "alb_security_group_id" {
  description = "ALB Security Group ID (Ingress annotation에 주입: alb.ingress.kubernetes.io/security-groups)"
  value       = aws_security_group.alb.id
}
