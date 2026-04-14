import re

with open('main.tf', 'r') as f:
    content = f.read()

# 1. Update Security Group (backend_sg) to allow self ingress (microservice to microservice)
backend_sg_old = """resource "aws_security_group" "backend_sg" {
  vpc_id      = aws_vpc.main.id
  name        = "${var.project_name}-backend-sg"
  description = "Backend ECS security group"

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id]
  }

  egress {"""
backend_sg_new = """resource "aws_security_group" "backend_sg" {
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

  egress {"""
content = content.replace(backend_sg_old, backend_sg_new)

# 2. Remove old ECR, Cloudwatch, ECS Task Def, ECS Service
ecs_pattern = r"###############################################################\n# ECR Repository\n###############################################################\nresource \"aws_ecr_repository\" \"backend\".*?(?=###############################################################\n# ACM \(us-east-1 for CloudFront\))"
content = re.sub(ecs_pattern, "", content, flags=re.DOTALL)

# 3. Append new MSA ECS Configuration
msa_content = """
###############################################################
# ECR Repositories (MSA)
###############################################################
locals {
  services = toset(["api-gateway", "user-service", "product-service", "order-service"])
}

resource "aws_ecr_repository" "msa" {
  for_each             = local.services
  name                 = "${var.project_name}-${each.key}"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

###############################################################
# CloudWatch Logs (MSA)
###############################################################
resource "aws_cloudwatch_log_group" "msa" {
  for_each          = local.services
  name              = "/ecs/${var.project_name}-${each.key}"
  retention_in_days = 7
}

###############################################################
# ECS Cluster & IAM Roles
###############################################################
resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-cluster"
}

resource "aws_iam_role" "ecs_task_execution_role" {
  name = "${var.project_name}-ecs-task-execution-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "ecs_task_role" {
  name = "${var.project_name}-ecs-task-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

###############################################################
# AWS Cloud Map (Service Discovery)
###############################################################
resource "aws_service_discovery_private_dns_namespace" "internal" {
  name        = "pposiraegi.internal"
  description = "Private DNS namespace for microservices"
  vpc         = aws_vpc.main.id
}

resource "aws_service_discovery_service" "msa" {
  for_each = local.services

  name = each.key

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.internal.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }
}

###############################################################
# ECS Task Definitions (MSA)
###############################################################
resource "aws_ecs_task_definition" "msa" {
  for_each                 = local.services
  family                   = "${var.project_name}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([{
    name      = each.key
    image     = "${aws_ecr_repository.msa[each.key].repository_url}:latest"
    essential = true
    portMappings = [{
      containerPort = 8080
      hostPort      = 8080
    }]
    environment = [
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
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.msa[each.key].name
        "awslogs-region"        = var.region
        "awslogs-stream-prefix" = "ecs"
      }
    }
  }])
}

###############################################################
# ECS Services (MSA)
###############################################################
resource "aws_ecs_service" "msa" {
  for_each        = local.services
  name            = "${var.project_name}-${each.key}-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.msa[each.key].arn
  launch_type     = "FARGATE"
  desired_count   = 1

  network_configuration {
    subnets          = [aws_subnet.private_a.id, aws_subnet.private_b.id]
    security_groups  = [aws_security_group.backend_sg.id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.msa[each.key].arn
  }

  # api-gateway만 ALB에 연결
  dynamic "load_balancer" {
    for_each = each.key == "api-gateway" ? [1] : []
    content {
      target_group_arn = aws_lb_target_group.backend_tg.arn
      container_name   = each.key
      container_port   = 8080
    }
  }

  depends_on = [
    aws_lb_listener.http
  ]
}
"""

# Insert new ECS configurations right before ACM block
idx = content.find("###############################################################\n# ACM (us-east-1 for CloudFront)")
content = content[:idx] + msa_content + content[idx:]

with open('main.tf', 'w') as f:
    f.write(content)
