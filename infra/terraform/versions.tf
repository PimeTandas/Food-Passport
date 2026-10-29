terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "aws" {
  region = var.region

  # Every resource gets these tags, so you can find (and cost) everything in the AWS console.
  default_tags {
    tags = {
      Project   = "food-passport"
      ManagedBy = "terraform"
    }
  }
}
