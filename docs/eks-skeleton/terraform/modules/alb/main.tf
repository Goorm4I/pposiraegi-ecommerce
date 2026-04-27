###############################################################
# ALB Module - Phase 3 EKS
# 실제 ALB는 AWS Load Balancer Controller + Ingress로 자동 생성
# Terraform에서는 ALB SG만 사전 생성 → Ingress annotation에 주입
###############################################################

resource "aws_security_group" "alb" {
  vpc_id      = var.vpc_id
  name        = "${var.project_name}-alb-sg"
  description = "ALB security group - managed by LBC Ingress annotation"

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
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
