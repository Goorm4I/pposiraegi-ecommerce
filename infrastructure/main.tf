###############################################################
# Provider
###############################################################
provider "aws" {
  region  = var.region
  profile = var.aws_profile
}

provider "aws" {
  alias   = "us_east_1"
  region  = "us-east-1"
  profile = var.aws_profile
}

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }
}

###############################################################
# Data
###############################################################
data "aws_availability_zones" "available" {}

###############################################################
# Modules
###############################################################

# 1. 네트워킹 (VPC, Subnet, Route Table, NAT)
module "networking" {
  source = "./modules/networking"

  project_name          = var.project_name
  vpc_cidr              = var.vpc_cidr
  public_subnet_a_cidr  = var.public_subnet_a_cidr
  public_subnet_b_cidr  = var.public_subnet_b_cidr
  private_subnet_a_cidr = var.private_subnet_a_cidr
  private_subnet_b_cidr = var.private_subnet_b_cidr
  azs                   = data.aws_availability_zones.available.names
}

# 2. 보안 그룹 (Security Groups)
module "security" {
  source = "./modules/security"

  project_name = var.project_name
  vpc_id       = module.networking.vpc_id
  eks_cluster_sg_id = module.eks.cluster_security_group_id
}

# 3. 스토리지 (RDS, ElastiCache, S3, SSM)
module "storage" {
  source = "./modules/storage"

  project_name       = var.project_name
  private_subnet_ids = [module.networking.private_subnet_a_id, module.networking.private_subnet_b_id]
  rds_sg_id          = module.security.rds_sg_id
  redis_sg_id        = module.security.redis_sg_id
  db_username        = var.db_username
  db_password        = var.db_password
  jwt_secret         = var.jwt_secret
}

# 4. EKS 클러스터
module "eks" {
  source = "./modules/eks"

  project_name       = var.project_name
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = [module.networking.private_subnet_a_id, module.networking.private_subnet_b_id]
  public_subnet_ids  = [module.networking.public_subnet_a_id, module.networking.public_subnet_b_id]
  cluster_version    = var.eks_cluster_version
  node_instance_type = var.eks_node_instance_type
  node_desired_size  = var.eks_node_desired_size
  node_min_size      = var.eks_node_min_size
  node_max_size      = var.eks_node_max_size
}

###############################################################
# ALB (EKS Ingress Controller에서 재사용)
###############################################################
resource "aws_lb" "alb" {
  name               = "${var.project_name}-alb"
  load_balancer_type = "application"
  security_groups    = [module.security.alb_sg_id]
  subnets            = [module.networking.public_subnet_a_id, module.networking.public_subnet_b_id]

  tags = { Name = "${var.project_name}-alb" }
}

resource "aws_lb_target_group" "backend_tg" {
  name        = "${var.project_name}-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = module.networking.vpc_id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    interval            = 30
    timeout             = 10
    healthy_threshold   = 2
    unhealthy_threshold = 3
    matcher             = "200-499"
  }

  tags = { Name = "${var.project_name}-tg" }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.alb.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend_tg.arn
  }
}

###############################################################
# CloudFront OAC
###############################################################
resource "aws_cloudfront_origin_access_control" "frontend_oac" {
  name                              = "${var.project_name}-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

###############################################################
# CloudFront Distribution
###############################################################
resource "aws_cloudfront_distribution" "frontend" {
  enabled             = true
  default_root_object = "index.html"
  price_class         = "PriceClass_All"
  aliases             = [var.domain_name, "www.${var.domain_name}"]

  origin {
    domain_name              = module.storage.frontend_bucket_domain
    origin_id                = "s3-frontend"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend_oac.id
  }

  origin {
    domain_name = aws_lb.alb.dns_name
    origin_id   = "alb-backend"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  ordered_cache_behavior {
    path_pattern           = "/api/*"
    target_origin_id       = "alb-backend"
    viewer_protocol_policy = "allow-all"
    allowed_methods        = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods         = ["GET", "HEAD"]

    forwarded_values {
      query_string = true
      headers      = ["Authorization", "Content-Type", "Origin", "Accept"]
      cookies { forward = "none" }
    }

    min_ttl     = 0
    default_ttl = 0
    max_ttl     = 0
  }

  default_cache_behavior {
    target_origin_id       = "s3-frontend"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]

    forwarded_values {
      query_string = false
      cookies { forward = "none" }
    }

    min_ttl     = 0
    default_ttl = 3600
    max_ttl     = 86400
  }

  custom_error_response {
    error_code         = 403
    response_code      = 200
    response_page_path = "/index.html"
  }

  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
  }

  restrictions {
    geo_restriction { restriction_type = "none" }
  }

  viewer_certificate {
    acm_certificate_arn      = aws_acm_certificate_validation.cert.certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  tags = { Name = "${var.project_name}-cf" }
}

###############################################################
# S3 Bucket Policy (CloudFront OAC 접근 허용)
###############################################################
resource "aws_s3_bucket_policy" "frontend" {
  bucket = module.storage.frontend_bucket_name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "cloudfront.amazonaws.com" }
      Action    = "s3:GetObject"
      Resource  = "${module.storage.frontend_bucket_arn}/*"
      Condition = {
        StringEquals = {
          "AWS:SourceArn" = aws_cloudfront_distribution.frontend.arn
        }
      }
    }]
  })
}

###############################################################
# ECR Repositories (MSA 이미지 저장소 - EKS에서 재사용)
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
# CloudWatch Log Groups (EKS 서비스 로그용)
###############################################################
resource "aws_cloudwatch_log_group" "msa" {
  for_each          = local.services
  name              = "/eks/${var.project_name}-${each.key}"
  retention_in_days = 7
}

###############################################################
# ACM (us-east-1 for CloudFront)
###############################################################
resource "aws_acm_certificate" "cert" {
  provider                  = aws.us_east_1
  domain_name               = var.domain_name
  subject_alternative_names = ["*.${var.domain_name}"]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = { Name = "${var.project_name}-cert" }
}

###############################################################
# Route53
###############################################################
data "aws_route53_zone" "main" {
  name         = var.domain_name
  private_zone = false
}

resource "aws_route53_record" "cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.cert.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  zone_id         = data.aws_route53_zone.main.zone_id
  name            = each.value.name
  type            = each.value.type
  ttl             = 60
  records         = [each.value.record]
  allow_overwrite = true
}

resource "aws_acm_certificate_validation" "cert" {
  provider                = aws.us_east_1
  certificate_arn         = aws_acm_certificate.cert.arn
  validation_record_fqdns = [for r in aws_route53_record.cert_validation : r.fqdn]
}

resource "aws_route53_record" "root" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.frontend.domain_name
    zone_id                = aws_cloudfront_distribution.frontend.hosted_zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "www" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "www.${var.domain_name}"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.frontend.domain_name
    zone_id                = aws_cloudfront_distribution.frontend.hosted_zone_id
    evaluate_target_health = false
  }
}
