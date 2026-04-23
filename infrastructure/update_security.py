import re

with open('main.tf', 'r') as f:
    content = f.read()

# 1. Replace backend_sg with api_gateway_sg and internal_msa_sg
sg_old = """resource "aws_security_group" "backend_sg" {
  vpc_id      = aws_vpc.main.id
  name        = "${var.project_name}-backend-sg"
  description = "Backend ECS security group"

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }

  ingress {
    from_port = 8080
    to_port   = 8080
    protocol  = "tcp"
    self      = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-backend-sg" }
}"""
sg_new = """resource "aws_security_group" "api_gateway_sg" {
  vpc_id      = aws_vpc.main.id
  name        = "${var.project_name}-api-gateway-sg"
  description = "API Gateway ECS security group"

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-api-gateway-sg" }
}

resource "aws_security_group" "internal_msa_sg" {
  vpc_id      = aws_vpc.main.id
  name        = "${var.project_name}-internal-msa-sg"
  description = "Internal MSA ECS security group"

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.api_gateway_sg.id]
  }

  ingress {
    from_port = 8080
    to_port   = 8080
    protocol  = "tcp"
    self      = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-internal-msa-sg" }
}"""
content = content.replace(sg_old, sg_new)

# 2. Update RDS and Redis Security Groups to allow both
rds_sg_old = """resource "aws_security_group" "rds_sg" {
  vpc_id      = aws_vpc.main.id
  name        = "${var.project_name}-rds-sg"
  description = "RDS PostgreSQL security group"

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.backend_sg.id]
  }"""
rds_sg_new = """resource "aws_security_group" "rds_sg" {
  vpc_id      = aws_vpc.main.id
  name        = "${var.project_name}-rds-sg"
  description = "RDS PostgreSQL security group"

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.api_gateway_sg.id, aws_security_group.internal_msa_sg.id]
  }"""
content = content.replace(rds_sg_old, rds_sg_new)

redis_sg_old = """resource "aws_security_group" "redis_sg" {
  vpc_id      = aws_vpc.main.id
  name        = "${var.project_name}-redis-sg"
  description = "ElastiCache Redis security group"

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.backend_sg.id]
  }"""
redis_sg_new = """resource "aws_security_group" "redis_sg" {
  vpc_id      = aws_vpc.main.id
  name        = "${var.project_name}-redis-sg"
  description = "ElastiCache Redis security group"

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.api_gateway_sg.id, aws_security_group.internal_msa_sg.id]
  }"""
content = content.replace(redis_sg_old, redis_sg_new)

# 3. Add SSM Parameters
ssm_content = """
###############################################################
# SSM Parameter Store (Secrets)
###############################################################
resource "aws_ssm_parameter" "jwt_secret" {
  name  = "/${var.project_name}/jwt_secret"
  type  = "SecureString"
  value = var.jwt_secret
}

resource "aws_ssm_parameter" "db_password" {
  name  = "/${var.project_name}/db_password"
  type  = "SecureString"
  value = var.db_password
}
"""
idx = content.find("###############################################################\n# ECR Repositories (MSA)")
content = content[:idx] + ssm_content + content[idx:]

# 4. Update IAM Roles for SSM
iam_old = """resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}"""
iam_new = """resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "ecs_task_execution_ssm_policy" {
  name   = "${var.project_name}-ecs-ssm-policy"
  role   = aws_iam_role.ecs_task_execution_role.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["ssm:GetParameters"]
      Resource = [
        aws_ssm_parameter.jwt_secret.arn,
        aws_ssm_parameter.db_password.arn
      ]
    }]
  })
}"""
content = content.replace(iam_old, iam_new)

# 5. Update Task Definition environment & secrets
task_def_old = """    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
      { name = "DB_HOST", value = aws_db_instance.postgres.address },
      { name = "DB_USERNAME", value = var.db_username },
      { name = "DB_PASSWORD", value = var.db_password },
      { name = "REDIS_HOST", value = aws_elasticache_cluster.redis.cache_nodes[0].address },
      { name = "JWT_SECRET", value = var.jwt_secret },
      { name = "CORS_ALLOWED_ORIGINS", value = "https://${aws_cloudfront_distribution.frontend.domain_name}" },
      { name = "USER_SERVICE_URL", value = "http://user-service.pposiraegi.internal:8080" },
      { name = "PRODUCT_SERVICE_URL", value = "http://product-service.pposiraegi.internal:8080" },
      { name = "ORDER_SERVICE_URL", value = "http://order-service.pposiraegi.internal:8080" }
    ]
    logConfiguration = {"""
task_def_new = """    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
      { name = "DB_HOST", value = aws_db_instance.postgres.address },
      { name = "DB_USERNAME", value = var.db_username },
      { name = "REDIS_HOST", value = aws_elasticache_cluster.redis.cache_nodes[0].address },
      { name = "CORS_ALLOWED_ORIGINS", value = "https://${aws_cloudfront_distribution.frontend.domain_name}" },
      { name = "USER_SERVICE_URL", value = "http://user-service.pposiraegi.internal:8080" },
      { name = "PRODUCT_SERVICE_URL", value = "http://product-service.pposiraegi.internal:8080" },
      { name = "ORDER_SERVICE_URL", value = "http://order-service.pposiraegi.internal:8080" }
    ]
    secrets = [
      { name = "DB_PASSWORD", valueFrom = aws_ssm_parameter.db_password.arn },
      { name = "JWT_SECRET", valueFrom = aws_ssm_parameter.jwt_secret.arn }
    ]
    logConfiguration = {"""
content = content.replace(task_def_old, task_def_new)

# 6. Update ECS Service SG Logic
ecs_sg_old = """  network_configuration {
    subnets          = [aws_subnet.private_a.id, aws_subnet.private_b.id]
    security_groups  = [aws_security_group.backend_sg.id]
    assign_public_ip = false
  }"""
ecs_sg_new = """  network_configuration {
    subnets          = [aws_subnet.private_a.id, aws_subnet.private_b.id]
    security_groups  = each.key == "api-gateway" ? [aws_security_group.api_gateway_sg.id] : [aws_security_group.internal_msa_sg.id]
    assign_public_ip = false
  }"""
content = content.replace(ecs_sg_old, ecs_sg_new)

with open('main.tf', 'w') as f:
    f.write(content)
