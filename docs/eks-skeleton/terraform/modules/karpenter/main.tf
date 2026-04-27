###############################################################
# Karpenter Module - Phase 3 EKS
# Karpenter Controller IRSA + 노드 프로비저닝 권한
###############################################################

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

###############################################################
# Karpenter Controller IAM Role (IRSA)
###############################################################
resource "aws_iam_role" "karpenter_controller" {
  name = "KarpenterController-${var.cluster_name}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = var.oidc_provider_arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "${var.oidc_provider_url}:sub" = "system:serviceaccount:karpenter:karpenter"
          "${var.oidc_provider_url}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "karpenter_controller" {
  name = "KarpenterControllerPolicy"
  role = aws_iam_role.karpenter_controller.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowScopedEC2InstanceAccessActions"
        Effect = "Allow"
        Action = [
          "ec2:RunInstances",
          "ec2:CreateFleet",
        ]
        Resource = [
          "arn:aws:ec2:${data.aws_region.current.name}::image/*",
          "arn:aws:ec2:${data.aws_region.current.name}::snapshot/*",
          "arn:aws:ec2:${data.aws_region.current.name}:*:security-group/*",
          "arn:aws:ec2:${data.aws_region.current.name}:*:subnet/*",
        ]
      },
      {
        Sid    = "AllowScopedEC2LaunchTemplateAccessActions"
        Effect = "Allow"
        Action = [
          "ec2:RunInstances",
          "ec2:CreateFleet",
        ]
        Resource = "arn:aws:ec2:${data.aws_region.current.name}:*:launch-template/*"
        Condition = {
          StringEquals = {
            "aws:ResourceTag/kubernetes.io/cluster/${var.cluster_name}" = "owned"
          }
        }
      },
      {
        Sid    = "AllowScopedEC2InstanceActionsWithTags"
        Effect = "Allow"
        Action = [
          "ec2:RunInstances",
          "ec2:CreateFleet",
          "ec2:CreateLaunchTemplate",
        ]
        Resource = [
          "arn:aws:ec2:${data.aws_region.current.name}:*:fleet/*",
          "arn:aws:ec2:${data.aws_region.current.name}:*:instance/*",
          "arn:aws:ec2:${data.aws_region.current.name}:*:volume/*",
          "arn:aws:ec2:${data.aws_region.current.name}:*:network-interface/*",
          "arn:aws:ec2:${data.aws_region.current.name}:*:launch-template/*",
          "arn:aws:ec2:${data.aws_region.current.name}:*:spot-instances-request/*",
        ]
        Condition = {
          StringEquals = {
            "aws:RequestTag/kubernetes.io/cluster/${var.cluster_name}" = "owned"
          }
        }
      },
      {
        Sid    = "AllowScopedResourceCreationTagging"
        Effect = "Allow"
        Action = "ec2:CreateTags"
        Resource = [
          "arn:aws:ec2:${data.aws_region.current.name}:*:fleet/*",
          "arn:aws:ec2:${data.aws_region.current.name}:*:instance/*",
          "arn:aws:ec2:${data.aws_region.current.name}:*:volume/*",
          "arn:aws:ec2:${data.aws_region.current.name}:*:network-interface/*",
          "arn:aws:ec2:${data.aws_region.current.name}:*:launch-template/*",
          "arn:aws:ec2:${data.aws_region.current.name}:*:spot-instances-request/*",
        ]
        Condition = {
          StringEquals = {
            "aws:RequestTag/kubernetes.io/cluster/${var.cluster_name}" = "owned"
            "ec2:CreateAction" = [
              "RunInstances",
              "CreateFleet",
              "CreateLaunchTemplate",
            ]
          }
        }
      },
      {
        Sid      = "AllowScopedResourceTagging"
        Effect   = "Allow"
        Action   = "ec2:CreateTags"
        Resource = "arn:aws:ec2:${data.aws_region.current.name}:*:instance/*"
        Condition = {
          StringEquals = {
            "aws:ResourceTag/kubernetes.io/cluster/${var.cluster_name}" = "owned"
          }
          StringLike = {
            "aws:RequestTag/eks:eks-cluster-name" = "*"
          }
        }
      },
      {
        Sid    = "AllowScopedDeletion"
        Effect = "Allow"
        Action = [
          "ec2:TerminateInstances",
          "ec2:DeleteLaunchTemplate",
        ]
        Resource = [
          "arn:aws:ec2:${data.aws_region.current.name}:*:instance/*",
          "arn:aws:ec2:${data.aws_region.current.name}:*:launch-template/*",
        ]
        Condition = {
          StringEquals = {
            "aws:ResourceTag/kubernetes.io/cluster/${var.cluster_name}" = "owned"
          }
        }
      },
      {
        Sid    = "AllowRegionalReadActions"
        Effect = "Allow"
        Action = [
          "ec2:DescribeAvailabilityZones",
          "ec2:DescribeImages",
          "ec2:DescribeInstances",
          "ec2:DescribeInstanceTypeOfferings",
          "ec2:DescribeInstanceTypes",
          "ec2:DescribeLaunchTemplates",
          "ec2:DescribeSecurityGroups",
          "ec2:DescribeSpotPriceHistory",
          "ec2:DescribeSubnets",
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "aws:RequestedRegion" = data.aws_region.current.name
          }
        }
      },
      {
        Sid      = "AllowSSMReadActions"
        Effect   = "Allow"
        Action   = "ssm:GetParameter"
        Resource = "arn:aws:ssm:${data.aws_region.current.name}::parameter/aws/service/*"
      },
      {
        Sid      = "AllowPricingReadActions"
        Effect   = "Allow"
        Action   = "pricing:GetProducts"
        Resource = "*"
      },
      {
        Sid    = "AllowInterruptionQueueActions"
        Effect = "Allow"
        Action = [
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes",
          "sqs:GetQueueUrl",
          "sqs:ReceiveMessage",
        ]
        Resource = aws_sqs_queue.karpenter_interruption.arn
      },
      {
        Sid    = "AllowPassingInstanceRole"
        Effect = "Allow"
        Action = "iam:PassRole"
        Resource = var.node_role_arn
        Condition = {
          StringEquals = {
            "iam:PassedToService" = "ec2.amazonaws.com"
          }
        }
      },
      {
        Sid      = "AllowScopedInstanceProfileCreationActions"
        Effect   = "Allow"
        Action   = "iam:CreateInstanceProfile"
        Resource = "*"
        Condition = {
          StringEquals = {
            "aws:RequestTag/kubernetes.io/cluster/${var.cluster_name}" = "owned"
          }
        }
      },
      {
        Sid    = "AllowScopedInstanceProfileTagActions"
        Effect = "Allow"
        Action = "iam:TagInstanceProfile"
        Resource = "*"
        Condition = {
          StringEquals = {
            "aws:ResourceTag/kubernetes.io/cluster/${var.cluster_name}" = "owned"
            "aws:RequestTag/kubernetes.io/cluster/${var.cluster_name}"  = "owned"
          }
        }
      },
      {
        Sid    = "AllowScopedInstanceProfileActions"
        Effect = "Allow"
        Action = [
          "iam:AddRoleToInstanceProfile",
          "iam:RemoveRoleFromInstanceProfile",
          "iam:DeleteInstanceProfile",
        ]
        Resource = "*"
        Condition = {
          StringEquals = {
            "aws:ResourceTag/kubernetes.io/cluster/${var.cluster_name}" = "owned"
          }
        }
      },
      {
        Sid      = "AllowInstanceProfileReadActions"
        Effect   = "Allow"
        Action   = "iam:GetInstanceProfile"
        Resource = "*"
      },
      {
        Sid    = "AllowAPIServerEndpointDiscovery"
        Effect = "Allow"
        Action = "eks:DescribeCluster"
        Resource = "arn:aws:eks:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:cluster/${var.cluster_name}"
      },
    ]
  })
}

###############################################################
# Spot Interruption SQS Queue
# EC2 Spot 중단 알림 → Karpenter가 graceful drain 수행
###############################################################
resource "aws_sqs_queue" "karpenter_interruption" {
  name                      = "${var.cluster_name}-karpenter-interruption"
  message_retention_seconds = 300
  sqs_managed_sse_enabled   = true

  tags = { Name = "${var.cluster_name}-karpenter-interruption" }
}

resource "aws_sqs_queue_policy" "karpenter_interruption" {
  queue_url = aws_sqs_queue.karpenter_interruption.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = ["events.amazonaws.com", "sqs.amazonaws.com"] }
        Action    = "sqs:SendMessage"
        Resource  = aws_sqs_queue.karpenter_interruption.arn
      }
    ]
  })
}

###############################################################
# EventBridge Rules → SQS (Spot 중단, 상태 변경 이벤트)
###############################################################
resource "aws_cloudwatch_event_rule" "spot_interruption" {
  name        = "${var.cluster_name}-spot-interruption"
  description = "EC2 Spot Instance Interruption Warning"

  event_pattern = jsonencode({
    source      = ["aws.ec2"]
    detail-type = ["EC2 Spot Instance Interruption Warning"]
  })
}

resource "aws_cloudwatch_event_target" "spot_interruption" {
  rule      = aws_cloudwatch_event_rule.spot_interruption.name
  target_id = "KarpenterInterruptionQueue"
  arn       = aws_sqs_queue.karpenter_interruption.arn
}

resource "aws_cloudwatch_event_rule" "instance_state_change" {
  name        = "${var.cluster_name}-instance-state-change"
  description = "EC2 Instance State-change Notification"

  event_pattern = jsonencode({
    source      = ["aws.ec2"]
    detail-type = ["EC2 Instance State-change Notification"]
  })
}

resource "aws_cloudwatch_event_target" "instance_state_change" {
  rule      = aws_cloudwatch_event_rule.instance_state_change.name
  target_id = "KarpenterInterruptionQueue"
  arn       = aws_sqs_queue.karpenter_interruption.arn
}

resource "aws_cloudwatch_event_rule" "rebalance_recommendation" {
  name        = "${var.cluster_name}-rebalance"
  description = "EC2 Instance Rebalance Recommendation"

  event_pattern = jsonencode({
    source      = ["aws.ec2"]
    detail-type = ["EC2 Instance Rebalance Recommendation"]
  })
}

resource "aws_cloudwatch_event_target" "rebalance_recommendation" {
  rule      = aws_cloudwatch_event_rule.rebalance_recommendation.name
  target_id = "KarpenterInterruptionQueue"
  arn       = aws_sqs_queue.karpenter_interruption.arn
}
