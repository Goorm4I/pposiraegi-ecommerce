provider "aws" {
  region  = "ap-northeast-2"
  # 필요에 따라 profile 지정 (예: profile = "goorm")
}

# AWS 계정 ID를 가져와 고유한 버킷 이름 생성에 활용
data "aws_caller_identity" "current" {}

# 테라폼 상태를 저장할 S3 버킷
resource "aws_s3_bucket" "terraform_state" {
  bucket = "pposiraegi-tf-state-${data.aws_caller_identity.current.account_id}"

  # 실수로 삭제되는 것을 방지
  lifecycle {
    prevent_destroy = true
  }
}

# 상태 파일의 변경 이력 관리를 위한 버킷 버전 관리 활성화
resource "aws_s3_bucket_versioning" "enabled" {
  bucket = aws_s3_bucket.terraform_state.id
  versioning_configuration {
    status = "Enabled"
  }
}

# 상태 파일 암호화 활성화
resource "aws_s3_bucket_server_side_encryption_configuration" "default" {
  bucket = aws_s3_bucket.terraform_state.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# 퍼블릭 액세스 차단
resource "aws_s3_bucket_public_access_block" "public_access" {
  bucket                  = aws_s3_bucket.terraform_state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# 동시 실행 방지를 위한 DynamoDB 테이블 (Locking)
resource "aws_dynamodb_table" "terraform_locks" {
  name         = "pposiraegi-tf-locks"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }
}

output "state_bucket_name" {
  value = aws_s3_bucket.terraform_state.bucket
}

output "dynamodb_table_name" {
  value = aws_dynamodb_table.terraform_locks.name
}