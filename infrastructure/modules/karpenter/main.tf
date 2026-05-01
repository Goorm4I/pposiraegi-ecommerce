###############################################################
# Karpenter Controller IAM Role (IRSA)
###############################################################
resource "aws_iam_service_linked_role" "spot" {
  aws_service_name = "spot.amazonaws.com"
}

data "aws_iam_policy_document" "karpenter_assume" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [var.oidc_provider_arn]
    }
    condition {
      test     = "StringEquals"
      variable = "${var.oidc_provider_url}:sub"
      values   = ["system:serviceaccount:karpenter:karpenter"]
    }
    condition {
      test     = "StringEquals"
      variable = "${var.oidc_provider_url}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "karpenter_controller" {
  name               = "${var.project_name}-karpenter-controller"
  assume_role_policy = data.aws_iam_policy_document.karpenter_assume.json
}

resource "aws_iam_role_policy" "karpenter_controller" {
  name = "${var.project_name}-karpenter-controller-policy"
  role = aws_iam_role.karpenter_controller.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowScopedEC2InstanceActions"
        Effect = "Allow"
        Action = [
          "ec2:RunInstances",
          "ec2:CreateLaunchTemplate",
          "ec2:CreateFleet",
        ]
        Resource = [
          "arn:aws:ec2:*::image/*",
          "arn:aws:ec2:*::snapshot/*",
          "arn:aws:ec2:*:*:spot-instances-request/*",
          "arn:aws:ec2:*:*:security-group/*",
          "arn:aws:ec2:*:*:subnet/*",
          "arn:aws:ec2:*:*:launch-template/*",
          "arn:aws:ec2:*:*:network-interface/*",
          "arn:aws:ec2:*:*:instance/*",
          "arn:aws:ec2:*:*:volume/*",
          "arn:aws:ec2:*:*:fleet/*",
        ]
      },
      {
        Sid    = "AllowScopedEC2InstanceActionsWithTags"
        Effect = "Allow"
        Action = [
          "ec2:RunInstances",
          "ec2:CreateLaunchTemplate",
          "ec2:CreateFleet",
          "ec2:TerminateInstances",
        ]
        Resource = ["*"]
        Condition = {
          StringLike = {
            # StringLike: 와일드카드 매칭 (StringEquals는 "*"를 리터럴로 해석)
            "aws:ResourceTag/karpenter.sh/nodepool" = "*"
          }
        }
      },
      {
        # DeleteLaunchTemplate은 생성 직후 태그가 없을 수 있어 조건 없이 허용
        Sid      = "AllowDeleteLaunchTemplate"
        Effect   = "Allow"
        Action   = ["ec2:DeleteLaunchTemplate"]
        Resource = "arn:aws:ec2:*:*:launch-template/*"
      },
      {
        # Spot 가격 데이터 조회 — 없으면 기본값 사용하지만 로그 노이즈 발생
        Sid      = "AllowPricingReadActions"
        Effect   = "Allow"
        Action   = ["pricing:GetProducts"]
        Resource = "*"
      },
      {
        Sid    = "AllowScopedResourceCreationTagging"
        Effect = "Allow"
        Action = ["ec2:CreateTags"]
        Resource = [
          "arn:aws:ec2:*:*:fleet/*",
          "arn:aws:ec2:*:*:instance/*",
          "arn:aws:ec2:*:*:volume/*",
          "arn:aws:ec2:*:*:network-interface/*",
          "arn:aws:ec2:*:*:launch-template/*",
          "arn:aws:ec2:*:*:spot-instances-request/*",
        ]
      },
      {
        Sid    = "AllowEC2ReadActions"
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
        Resource = ["*"]
      },
      {
        Sid    = "AllowSSMReadActions"
        Effect = "Allow"
        Action = ["ssm:GetParameter"]
        # al2023 AMI SSM 파라미터 조회
        Resource = ["arn:aws:ssm:*:*:parameter/aws/service/*"]
      },
      {
        Sid      = "AllowSQSActions"
        Effect   = "Allow"
        Action   = ["sqs:DeleteMessage", "sqs:GetQueueAttributes", "sqs:GetQueueUrl", "sqs:ReceiveMessage"]
        Resource = [aws_sqs_queue.karpenter_interruption.arn]
      },
      {
        Sid      = "AllowPassNodeIAMRole"
        Effect   = "Allow"
        Action   = ["iam:PassRole"]
        Resource = [var.node_role_arn]
      },
      {
        # Karpenter v1: EC2NodeClass Ready 상태를 위해 Instance Profile CRUD 필요
        Sid    = "AllowInstanceProfileActions"
        Effect = "Allow"
        Action = [
          "iam:CreateInstanceProfile",
          "iam:TagInstanceProfile",
          "iam:AddRoleToInstanceProfile",
          "iam:RemoveRoleFromInstanceProfile",
          "iam:DeleteInstanceProfile",
          "iam:GetInstanceProfile",
        ]
        Resource = "arn:aws:iam::*:instance-profile/*"
      },
      {
        Sid      = "AllowEKSReadActions"
        Effect   = "Allow"
        Action   = ["eks:DescribeCluster"]
        Resource = ["arn:aws:eks:*:*:cluster/${var.cluster_name}"]
      },
      {
        Sid      = "AllowInterruptionQueueActions"
        Effect   = "Allow"
        Action   = ["sqs:SendMessage"]
        Resource = [aws_sqs_queue.karpenter_interruption.arn]
      },
    ]
  })
}

###############################################################
# Node Instance Profile (ec2nodeclass.yaml role: 참조)
###############################################################
resource "aws_iam_instance_profile" "karpenter_node" {
  name = "${var.project_name}-eks-node-role"
  role = split("/", var.node_role_arn)[1]
}

###############################################################
# SQS — Spot 중단 이벤트 수신
###############################################################
resource "aws_sqs_queue" "karpenter_interruption" {
  name                      = "${var.project_name}-karpenter-interruption"
  message_retention_seconds = 300
  sqs_managed_sse_enabled   = true

  tags = { Name = "${var.project_name}-karpenter-interruption" }
}

data "aws_iam_policy_document" "karpenter_sqs" {
  statement {
    actions   = ["sqs:SendMessage"]
    resources = [aws_sqs_queue.karpenter_interruption.arn]
    principals {
      type        = "Service"
      identifiers = ["events.amazonaws.com", "sqs.amazonaws.com"]
    }
  }
}

resource "aws_sqs_queue_policy" "karpenter_interruption" {
  queue_url = aws_sqs_queue.karpenter_interruption.url
  policy    = data.aws_iam_policy_document.karpenter_sqs.json
}

###############################################################
# EventBridge — Spot 중단/Rebalance/상태변경 이벤트 → SQS
###############################################################
resource "aws_cloudwatch_event_rule" "spot_interruption" {
  name        = "${var.project_name}-karpenter-spot-interruption"
  description = "Karpenter: EC2 Spot Instance Interruption Warning"

  event_pattern = jsonencode({
    source      = ["aws.ec2"]
    detail-type = ["EC2 Spot Instance Interruption Warning"]
  })
}

resource "aws_cloudwatch_event_target" "spot_interruption" {
  rule      = aws_cloudwatch_event_rule.spot_interruption.name
  target_id = "KarpenterSQS"
  arn       = aws_sqs_queue.karpenter_interruption.arn
}

resource "aws_cloudwatch_event_rule" "rebalance" {
  name        = "${var.project_name}-karpenter-rebalance"
  description = "Karpenter: EC2 Instance Rebalance Recommendation"

  event_pattern = jsonencode({
    source      = ["aws.ec2"]
    detail-type = ["EC2 Instance Rebalance Recommendation"]
  })
}

resource "aws_cloudwatch_event_target" "rebalance" {
  rule      = aws_cloudwatch_event_rule.rebalance.name
  target_id = "KarpenterSQS"
  arn       = aws_sqs_queue.karpenter_interruption.arn
}

resource "aws_cloudwatch_event_rule" "instance_state_change" {
  name        = "${var.project_name}-karpenter-instance-state"
  description = "Karpenter: EC2 Instance State Change"

  event_pattern = jsonencode({
    source      = ["aws.ec2"]
    detail-type = ["EC2 Instance State-change Notification"]
  })
}

resource "aws_cloudwatch_event_target" "instance_state_change" {
  rule      = aws_cloudwatch_event_rule.instance_state_change.name
  target_id = "KarpenterSQS"
  arn       = aws_sqs_queue.karpenter_interruption.arn
}

###############################################################
# EKS Node Security Group (Karpenter discovery 태그 포함)
###############################################################
resource "aws_security_group" "eks_node" {
  vpc_id      = var.vpc_id
  name        = "${var.project_name}-eks-node-sg"
  description = "EKS node security group for Karpenter-managed nodes"

  # 노드 간 통신 (all)
  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  # kubelet (control plane → node)
  ingress {
    from_port   = 10250
    to_port     = 10250
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "kubelet API"
  }

  # Istio HBONE (ztunnel 간 mTLS)
  ingress {
    from_port   = 15008
    to_port     = 15008
    protocol    = "tcp"
    self        = true
    description = "Istio HBONE"
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
    "kubernetes.io/cluster/${var.cluster_name}" = "owned"
  }

  lifecycle {
    # Additional node-to-node rules are managed by standalone aws_security_group_rule resources.
    # Without this, Terraform treats those rules as drift on the inline ingress set.
    ignore_changes = [ingress]
  }
}

resource "aws_security_group_rule" "cluster_api_from_karpenter_nodes" {
  type                     = "ingress"
  from_port                = 443
  to_port                  = 443
  protocol                 = "tcp"
  security_group_id        = var.cluster_security_group_id
  source_security_group_id = aws_security_group.eks_node.id
  description              = "Allow Karpenter-managed nodes to join EKS API server"
}

resource "aws_security_group_rule" "cluster_nodes_from_karpenter_nodes" {
  type                     = "ingress"
  from_port                = 0
  to_port                  = 0
  protocol                 = "-1"
  security_group_id        = var.cluster_security_group_id
  source_security_group_id = aws_security_group.eks_node.id
  description              = "Allow pod/node traffic from Karpenter nodes to managed nodes"
}

resource "aws_security_group_rule" "karpenter_nodes_from_cluster_nodes" {
  type                     = "ingress"
  from_port                = 0
  to_port                  = 0
  protocol                 = "-1"
  security_group_id        = aws_security_group.eks_node.id
  source_security_group_id = var.cluster_security_group_id
  description              = "Allow pod/node traffic from managed nodes to Karpenter nodes"
}
