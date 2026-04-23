output "frontend_bucket_name" {
  value = aws_s3_bucket.frontend.bucket
}

output "frontend_bucket_arn" {
  value = aws_s3_bucket.frontend.arn
}

output "frontend_bucket_domain" {
  value = aws_s3_bucket.frontend.bucket_regional_domain_name
}

output "rds_endpoint" {
  value = aws_db_instance.postgres.address
}

output "rds_port" {
  value = aws_db_instance.postgres.port
}

output "redis_endpoint" {
  value = aws_elasticache_cluster.redis.cache_nodes[0].address
}

output "jwt_secret_arn" {
  value = aws_ssm_parameter.jwt_secret.arn
}

output "db_password_arn" {
  value = aws_ssm_parameter.db_password.arn
}
