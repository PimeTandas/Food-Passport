locals {
  name         = "food-passport"
  site_domain  = "${var.subdomain}.${var.hosted_zone_name}"
  param_prefix = "/${local.name}"
  account_id   = data.aws_caller_identity.current.account_id
}

data "aws_caller_identity" "current" {}

# ---------------------------------------------------------------------------------------------
# Network: the default VPC is fine for one server. Only HTTP (for the certificate challenge and
# the redirect) and HTTPS are open. No SSH: we manage the server through AWS Systems Manager.
# ---------------------------------------------------------------------------------------------
data "aws_vpc" "default" {
  default = true
}

resource "aws_security_group" "web" {
  name        = "${local.name}-web"
  description = "HTTP and HTTPS in, anything out"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description      = "HTTP (redirect to HTTPS and Lets Encrypt challenges)"
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  ingress {
    description      = "HTTPS"
    from_port        = 443
    to_port          = 443
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

# ---------------------------------------------------------------------------------------------
# Storage: one bucket for photos (the app's), one for operations (deploy files + DB backups).
# Both private; S3 encrypts at rest by default.
# ---------------------------------------------------------------------------------------------
resource "aws_s3_bucket" "photos" {
  bucket = "${local.name}-photos-${local.account_id}"
}

resource "aws_s3_bucket" "ops" {
  bucket = "${local.name}-ops-${local.account_id}"
}

resource "aws_s3_bucket_public_access_block" "photos" {
  bucket                  = aws_s3_bucket.photos.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_public_access_block" "ops" {
  bucket                  = aws_s3_bucket.ops.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "ops" {
  bucket = aws_s3_bucket.ops.id

  rule {
    id     = "expire-old-backups"
    status = "Enabled"
    filter {
      prefix = "backups/"
    }
    expiration {
      days = 30
    }
  }
}

# ---------------------------------------------------------------------------------------------
# Container registry: GitHub Actions pushes images here, the server pulls them.
# ---------------------------------------------------------------------------------------------
resource "aws_ecr_repository" "app" {
  name                 = local.name
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name
  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the 10 most recent images (old ones cost storage)"
      selection    = { tagStatus = "any", countType = "imageCountMoreThan", countNumber = 10 }
      action       = { type = "expire" }
    }]
  })
}

# ---------------------------------------------------------------------------------------------
# Secrets: generated once by Terraform, stored encrypted in SSM Parameter Store (free tier).
# NOTE: they are also in terraform.tfstate, so keep that file private (it is git-ignored).
# ---------------------------------------------------------------------------------------------
resource "random_password" "db" {
  length  = 32
  special = false
}

resource "random_password" "jwt" {
  length  = 64
  special = false
}

resource "aws_ssm_parameter" "db_password" {
  name  = "${local.param_prefix}/db-password"
  type  = "SecureString"
  value = random_password.db.result
}

resource "aws_ssm_parameter" "jwt_secret" {
  name  = "${local.param_prefix}/jwt-secret"
  type  = "SecureString"
  value = random_password.jwt.result
}
