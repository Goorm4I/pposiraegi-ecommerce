###############################################################
# ALB Security Group
###############################################################
resource "aws_security_group" "alb_sg" {
  vpc_id      = var.vpc_id
  name        = "${var.project_name}-alb-sg"
  description = "ALB security group"

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-alb-sg" }
}

###############################################################
# API Gateway Security Group
###############################################################
resource "aws_security_group" "api_gateway_sg" {
  vpc_id      = var.vpc_id
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

###############################################################
# Internal MSA Security Group
###############################################################
resource "aws_security_group" "internal_msa_sg" {
  vpc_id      = var.vpc_id
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

  # gRPC 내부 통신
  ingress {
    from_port = 9090
    to_port   = 9090
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
}

###############################################################
# RDS Security Group
###############################################################
resource "aws_security_group" "rds_sg" {
  vpc_id      = var.vpc_id
  name        = "${var.project_name}-rds-sg"
  description = "RDS PostgreSQL security group"

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.api_gateway_sg.id, aws_security_group.internal_msa_sg.id]
  }

  # EKS/Karpenter 노드 접근 허용
   ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.eks_node_sg_id, var.eks_cluster_sg_id]
  }
  
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-rds-sg" }
}

###############################################################
# Redis Security Group
###############################################################
resource "aws_security_group" "redis_sg" {
  vpc_id      = var.vpc_id
  name        = "${var.project_name}-redis-sg"
  description = "ElastiCache Redis security group"

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.api_gateway_sg.id, aws_security_group.internal_msa_sg.id]
  }

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [var.eks_node_sg_id, var.eks_cluster_sg_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-redis-sg" }
}
