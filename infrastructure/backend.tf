terraform {
  backend "s3" {
    bucket         = "pposiraegi-tf-state-779846782353"
    key            = "ecommerce/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "pposiraegi-tf-locks"
    encrypt        = true
  }
}
