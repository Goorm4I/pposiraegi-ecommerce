output "lbc_role_arn" {
  description = "AWS Load Balancer Controller IAM Role ARN"
  value       = aws_iam_role.lbc.arn
}

output "eso_role_arn" {
  description = "External Secrets Operator IAM Role ARN"
  value       = aws_iam_role.eso.arn
}

output "service_role_arns" {
  description = "앱 서비스별 IAM Role ARN 맵"
  value       = { for k, v in aws_iam_role.service : k => v.arn }
}
