###############################################################
# Security Module - Phase 3 EKS
###############################################################

###############################################################
# EKS Node Security Group
# Karpenter EC2NodeClass에서 additionalSecurityGroupSelectorTerms로 참조
###############################################################
resource "aws_security_group" "eks_node" {
  vpc_id      = var.vpc_id
  name        = "${var.project_name}-eks-node-sg"
  description = "EKS worker node additional security group"

  ingress {
    description = "VPC 내부 전체 허용 (파드 간 통신)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name                                        = "${var.project_name}-eks-node-sg"
    "karpenter.sh/discovery"                    = var.cluster_name
  }
}

###############################################################
# RDS Security Group
###############################################################
resource "aws_security_group" "rds" {
  vpc_id      = var.vpc_id
  name        = "${var.project_name}-rds-sg"
  description = "RDS PostgreSQL security group"

  ingress {
    description     = "PostgreSQL from EKS nodes"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_node.id]
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
# Redis (ElastiCache) Security Group
###############################################################
resource "aws_security_group" "redis" {
  vpc_id      = var.vpc_id
  name        = "${var.project_name}-redis-sg"
  description = "ElastiCache Redis security group"

  ingress {
    description     = "Redis from EKS nodes"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.eks_node.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-redis-sg" }
}
