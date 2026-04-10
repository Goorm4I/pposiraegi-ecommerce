output "cloudfront_url" {
  description = "프론트엔드 접속 URL (CloudFront)"
  value       = "https://${aws_cloudfront_distribution.frontend.domain_name}"
}

output "alb_dns" {
  description = "백엔드 ALB DNS (API 직접 접근용)"
  value       = "http://${aws_lb.alb.dns_name}"
}

output "backend_public_ip" {
  description = "백엔드 EC2 퍼블릭 IP (SSH 접근용)"
  value       = aws_instance.backend.public_ip
}

output "s3_bucket_name" {
  description = "프론트엔드 S3 버킷명 (빌드 파일 업로드용)"
  value       = aws_s3_bucket.frontend.bucket
}

output "ssh_command" {
  description = "EC2 SSH 접속 명령어"
  value       = "ssh -i ~/.ssh/id_ed25519 ec2-user@${aws_instance.backend.public_ip}"
}

output "frontend_deploy_command" {
  description = "프론트엔드 S3 배포 명령어"
  value       = "aws s3 sync ./build s3://${aws_s3_bucket.frontend.bucket} --profile goorm --delete"
}

output "elasticache_endpoint" {
  description = "ElastiCache Redis 엔드포인트"
  value       = aws_elasticache_cluster.redis.cache_nodes[0].address
}

output "rds_endpoint" {
  description = "RDS PostgreSQL 엔드포인트"
  value       = aws_db_instance.postgres.address
}

output "rds_port" {
  description = "RDS PostgreSQL 포트"
  value       = aws_db_instance.postgres.port
}
