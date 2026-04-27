output "controller_role_arn" {
  description = "Karpenter Controller IAM Role ARN (IRSA)"
  value       = aws_iam_role.karpenter_controller.arn
}

output "interruption_queue_name" {
  description = "Spot 중단 SQS Queue 이름"
  value       = aws_sqs_queue.karpenter_interruption.name
}

output "interruption_queue_url" {
  description = "Spot 중단 SQS Queue URL"
  value       = aws_sqs_queue.karpenter_interruption.url
}
