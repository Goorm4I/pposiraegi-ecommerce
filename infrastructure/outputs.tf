output "cloudfront_url" {
  description = "프론트엔드 접속 URL (CloudFront)"
  value       = "https://${aws_cloudfront_distribution.frontend.domain_name}"
}

output "cloudfront_id" {
  description = "CloudFront Distribution ID (캐시 무효화용)"
  value       = aws_cloudfront_distribution.frontend.id
}

output "alb_dns" {
  description = "백엔드 ALB DNS (API 직접 접근용)"
  value       = "http://${aws_lb.alb.dns_name}"
}

output "s3_bucket_name" {
  description = "프론트엔드 S3 버킷명 (빌드 파일 업로드용)"
  value       = aws_s3_bucket.frontend.bucket
}

output "ecr_repository_urls" {
  description = "백엔드 MSA ECR 레포지토리 URL 목록"
  value       = { for k, v in aws_ecr_repository.msa : k => v.repository_url }
}

output "frontend_deploy_command" {
  description = "프론트엔드 S3 배포 명령어"
  value       = "aws s3 sync ./build s3://${aws_s3_bucket.frontend.bucket} --profile goorm --delete"
}

output "backend_push_command" {
  description = "백엔드 ECR 푸시 예시 명령어 (api-gateway 예시)"
  value       = "docker tag api-gateway:latest ${aws_ecr_repository.msa["api-gateway"].repository_url}:latest && docker push ${aws_ecr_repository.msa["api-gateway"].repository_url}:latest"
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
