terraform {
  backend "s3" {
<<<<<<< HEAD
    bucket         = "pposiraegi-tfstate-779846782353"
    key            = "infrastructure/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "pposiraegi-tf-locks"
    encrypt        = true
  }
}
=======
    bucket  = "pposiraegi-tfstate-779846782353"
    key     = "infrastructure/terraform.tfstate"
    region  = "ap-northeast-2"
    profile = "goorm"
  }
}
>>>>>>> origin/feat/eks-migration
