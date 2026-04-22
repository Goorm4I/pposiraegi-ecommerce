output "alb_sg_id" {
  value = aws_security_group.alb_sg.id
}

output "api_gateway_sg_id" {
  value = aws_security_group.api_gateway_sg.id
}

output "internal_msa_sg_id" {
  value = aws_security_group.internal_msa_sg.id
}

output "rds_sg_id" {
  value = aws_security_group.rds_sg.id
}

output "redis_sg_id" {
  value = aws_security_group.redis_sg.id
}
