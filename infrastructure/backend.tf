terraform {
  backend "s3" {
    bucket  = "pposiraegi-tfstate-779846782353"
    key     = "infrastructure/terraform.tfstate"
    region  = "ap-northeast-2"
    profile = "goorm"
  }
}