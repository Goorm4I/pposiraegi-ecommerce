output "controller_role_arn" {
  value = aws_iam_role.karpenter_controller.arn
}

output "interruption_queue_name" {
  value = aws_sqs_queue.karpenter_interruption.name
}

output "node_sg_id" {
  value       = aws_security_group.eks_node.id
  description = "EKS 노드 SG ID — RDS/Redis 인바운드 규칙에서 sg-0aa36452edaf69dd9 대체용"
}
