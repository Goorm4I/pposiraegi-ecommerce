###############################################################
# EKS Module - Phase 3
# 관리형 노드그룹은 system 컴포넌트(Karpenter, ArgoCD 등)용으로만 사용
# 앱 워크로드는 Karpenter Spot 노드에서 실행
###############################################################

###############################################################
# EKS Cluster IAM Role
###############################################################
resource "aws_iam_role" "cluster" {
  name = "${var.cluster_name}-cluster-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "eks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "cluster_policy" {
  role       = aws_iam_role.cluster.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

###############################################################
# EKS Cluster
###############################################################
resource "aws_eks_cluster" "main" {
  name    = var.cluster_name
  version = var.kubernetes_version
  role_arn = aws_iam_role.cluster.arn

  vpc_config {
    subnet_ids              = concat(var.public_subnet_ids, var.private_subnet_ids)
    endpoint_private_access = true
    endpoint_public_access  = true
    public_access_cidrs     = var.public_access_cidrs
  }

  access_config {
    authentication_mode                         = "API_AND_CONFIG_MAP"
    bootstrap_cluster_creator_admin_permissions = true
  }

  tags = { Name = var.cluster_name }

  depends_on = [aws_iam_role_policy_attachment.cluster_policy]
}

###############################################################
# OIDC Provider (IRSA, Karpenter IRSA 필수)
###############################################################
data "tls_certificate" "eks" {
  url = aws_eks_cluster.main.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "eks" {
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks.certificates[0].sha1_fingerprint]
  url             = aws_eks_cluster.main.identity[0].oidc[0].issuer

  tags = { Name = "${var.cluster_name}-oidc" }
}

###############################################################
# System Node Group IAM Role (Karpenter, ArgoCD, Istio 등)
###############################################################
resource "aws_iam_role" "node" {
  name = "${var.cluster_name}-node-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "node_policies" {
  for_each = toset([
    "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy",
    "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy",
    "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly",
    "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
  ])

  role       = aws_iam_role.node.name
  policy_arn = each.value
}

###############################################################
# System Node Group (On-Demand, 시스템 컴포넌트 전용)
###############################################################
resource "aws_eks_node_group" "system" {
  cluster_name    = aws_eks_cluster.main.name
  node_group_name = "${var.cluster_name}-system"
  node_role_arn   = aws_iam_role.node.arn
  subnet_ids      = var.private_subnet_ids

  instance_types = ["t3.medium"]
  capacity_type  = "ON_DEMAND"

  scaling_config {
    desired_size = 2
    min_size     = 2
    max_size     = 4
  }

  update_config {
    max_unavailable = 1
  }

  labels = {
    "node-role" = "system"
  }

  taint {
    key    = "CriticalAddonsOnly"
    value  = "true"
    effect = "NO_SCHEDULE"
  }

  tags = { Name = "${var.cluster_name}-system-node" }

  depends_on = [aws_iam_role_policy_attachment.node_policies]
}

###############################################################
# EKS Add-ons
###############################################################
resource "aws_eks_addon" "vpc_cni" {
  cluster_name             = aws_eks_cluster.main.name
  addon_name               = "vpc-cni"
  resolve_conflicts_on_update = "OVERWRITE"
}

resource "aws_eks_addon" "coredns" {
  cluster_name             = aws_eks_cluster.main.name
  addon_name               = "coredns"
  resolve_conflicts_on_update = "OVERWRITE"
  depends_on               = [aws_eks_node_group.system]
}

resource "aws_eks_addon" "kube_proxy" {
  cluster_name             = aws_eks_cluster.main.name
  addon_name               = "kube-proxy"
  resolve_conflicts_on_update = "OVERWRITE"
}

resource "aws_eks_addon" "pod_identity" {
  cluster_name             = aws_eks_cluster.main.name
  addon_name               = "eks-pod-identity-agent"
  resolve_conflicts_on_update = "OVERWRITE"
}
