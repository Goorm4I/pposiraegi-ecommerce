###############################################################
# ElastiCache Module - Redis (Phase 3 EKS)
###############################################################

resource "aws_elasticache_subnet_group" "main" {
  name       = "${var.project_name}-redis-subnet-group"
  subnet_ids = var.subnet_ids

  tags = { Name = "${var.project_name}-redis-subnet-group" }
}

resource "aws_elasticache_replication_group" "main" {
  replication_group_id = "${var.project_name}-redis"
  description          = "${var.project_name} Redis cluster"

  node_type            = var.node_type
  num_cache_clusters   = var.num_cache_clusters
  port                 = 6379

  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [var.security_group_id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = false  # 앱에서 TLS 미설정 시 false

  automatic_failover_enabled = var.num_cache_clusters > 1 ? true : false

  tags = { Name = "${var.project_name}-redis" }
}
