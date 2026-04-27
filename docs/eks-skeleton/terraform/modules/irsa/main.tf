###############################################################
# IRSA Module - 서비스별 Pod IAM Role (IRSA)
# 각 서비스 SA에 최소 권한 IAM Role 부여
###############################################################

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

###############################################################
# AWS Load Balancer Controller IRSA
###############################################################
resource "aws_iam_role" "lbc" {
  name = "AWSLoadBalancerController-${var.cluster_name}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = { Federated = var.oidc_provider_arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${var.oidc_provider_url}:sub" = "system:serviceaccount:kube-system:aws-load-balancer-controller"
          "${var.oidc_provider_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "lbc" {
  role       = aws_iam_role.lbc.name
  policy_arn = aws_iam_policy.lbc.arn
}

resource "aws_iam_policy" "lbc" {
  name        = "AWSLoadBalancerControllerIAMPolicy-${var.cluster_name}"
  description = "AWS Load Balancer Controller IAM Policy"

  policy = file("${path.module}/policies/lbc-policy.json")
}

###############################################################
# External Secrets Operator IRSA
# Secrets Manager 읽기 전용
###############################################################
resource "aws_iam_role" "eso" {
  name = "ExternalSecretsOperator-${var.cluster_name}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = { Federated = var.oidc_provider_arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${var.oidc_provider_url}:sub" = "system:serviceaccount:external-secrets:external-secrets"
          "${var.oidc_provider_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "eso" {
  name = "ExternalSecretsPolicy"
  role = aws_iam_role.eso.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
          "secretsmanager:ListSecretVersionIds",
        ]
        Resource = "arn:aws:secretsmanager:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:secret:${var.cluster_name}/*"
      }
    ]
  })
}

###############################################################
# 앱 서비스별 IRSA (order, product, user)
# - 결제 PG는 외부 위젯(프론트 ↔ PG사)에서 처리되고, 백엔드는
#   order-service 내부 PaymentClient로 검증만 수행 → payment-service 없음
# - SQS는 결제 완료 후 side-effect 이벤트만 사용하므로 order-sa만 권한 보유
###############################################################
locals {
  services = {
    order   = { namespace = "default", permissions = ["sqs"] }
    product = { namespace = "default", permissions = [] }
    user    = { namespace = "default", permissions = [] }
  }
}

resource "aws_iam_role" "service" {
  for_each = local.services

  name = "${var.cluster_name}-${each.key}-service-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = { Federated = var.oidc_provider_arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${var.oidc_provider_url}:sub" = "system:serviceaccount:${each.value.namespace}:${each.key}-service"
          "${var.oidc_provider_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })

  tags = { Name = "${var.cluster_name}-${each.key}-service-role" }
}

# order 서비스 SQS 접근 권한 (결제 완료 후 side-effect 이벤트 publish/consume)
resource "aws_iam_role_policy" "service_sqs" {
  for_each = { for k, v in local.services : k => v if contains(v.permissions, "sqs") }

  name = "SQSPolicy"
  role = aws_iam_role.service[each.key].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes",
        ]
        Resource = "arn:aws:sqs:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:${var.cluster_name}-*"
      }
    ]
  })
}
