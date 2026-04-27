###############################################################
# Phase 3 EKS - Dev Environment
###############################################################

terraform {
  required_version = ">= 1.6"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

###############################################################
# VPC
###############################################################
module "vpc" {
  source = "../../modules/vpc"

  project_name = var.project_name
  cluster_name = var.cluster_name
  vpc_cidr     = var.vpc_cidr

  public_subnets = {
    a = { cidr = "10.0.1.0/24", az = "ap-northeast-2a" }
    b = { cidr = "10.0.2.0/24", az = "ap-northeast-2c" }
  }

  private_subnets = {
    a = { cidr = "10.0.3.0/24", az = "ap-northeast-2a" }
    b = { cidr = "10.0.4.0/24", az = "ap-northeast-2c" }
  }
}

###############################################################
# Security Groups
###############################################################
module "security" {
  source = "../../modules/security"

  project_name = var.project_name
  cluster_name = var.cluster_name
  vpc_id       = module.vpc.vpc_id
  vpc_cidr     = module.vpc.vpc_cidr_block
}

###############################################################
# ALB Security Group (LBC Ingress에서 사용)
###############################################################
module "alb" {
  source = "../../modules/alb"

  project_name = var.project_name
  vpc_id       = module.vpc.vpc_id
}

###############################################################
# RDS
###############################################################
module "rds" {
  source = "../../modules/rds"

  project_name      = var.project_name
  subnet_ids        = module.vpc.private_subnet_ids
  security_group_id = module.security.rds_security_group_id
  db_password       = var.db_password

  instance_class      = "db.t3.micro"
  allocated_storage   = 20
  deletion_protection = false
}

###############################################################
# ElastiCache Redis
###############################################################
module "elasticache" {
  source = "../../modules/elasticache"

  project_name      = var.project_name
  subnet_ids        = module.vpc.private_subnet_ids
  security_group_id = module.security.redis_security_group_id

  node_type          = "cache.t3.micro"
  num_cache_clusters = 1
}

###############################################################
# EKS Cluster
###############################################################
module "eks" {
  source = "../../modules/eks"

  cluster_name       = var.cluster_name
  kubernetes_version = "1.31"
  public_subnet_ids  = module.vpc.public_subnet_ids
  private_subnet_ids = module.vpc.private_subnet_ids
}

###############################################################
# Karpenter (Controller IRSA + Spot SQS)
###############################################################
module "karpenter" {
  source = "../../modules/karpenter"

  cluster_name      = var.cluster_name
  oidc_provider_arn = module.eks.oidc_provider_arn
  oidc_provider_url = module.eks.oidc_provider_url
  node_role_arn     = module.eks.node_role_arn
}

###############################################################
# IRSA (서비스별 Pod IAM Role)
###############################################################
module "irsa" {
  source = "../../modules/irsa"

  cluster_name      = var.cluster_name
  oidc_provider_arn = module.eks.oidc_provider_arn
  oidc_provider_url = module.eks.oidc_provider_url
}
