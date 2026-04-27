output "redis_primary_endpoint" {
  description = "Redis Primary 엔드포인트"
  value       = aws_elasticache_replication_group.main.primary_endpoint_address
}

output "redis_reader_endpoint" {
  description = "Redis Reader 엔드포인트"
  value       = aws_elasticache_replication_group.main.reader_endpoint_address
}

output "redis_port" {
  description = "Redis 포트"
  value       = 6379
}
