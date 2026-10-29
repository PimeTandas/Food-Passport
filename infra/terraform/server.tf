# The latest Amazon Linux 2023 image, looked up from AWS's public parameter.
data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

resource "aws_instance" "server" {
  ami                    = data.aws_ssm_parameter.al2023.insecure_value
  instance_type          = var.instance_type
  iam_instance_profile   = aws_iam_instance_profile.server.name
  vpc_security_group_ids = [aws_security_group.web.id]

  # "standard" means the instance slows down when it runs out of CPU credits, instead of
  # quietly billing you for extra ("unlimited" is the t3 default).
  credit_specification {
    cpu_credits = "standard"
  }

  metadata_options {
    http_tokens                 = "required" # IMDSv2 only
    http_put_response_hop_limit = 2          # containers sit one network hop away and need the role too
  }

  root_block_device {
    volume_size = 20 # GB; free tier covers 30
    volume_type = "gp3"
    encrypted   = true
  }

  user_data = templatefile("${path.module}/server-init.sh.tftpl", {
    region             = var.region
    ecr_repository_url = aws_ecr_repository.app.repository_url
    photos_bucket      = aws_s3_bucket.photos.bucket
    ops_bucket         = aws_s3_bucket.ops.bucket
    param_prefix       = local.param_prefix
    site_domain        = local.site_domain
    acme_email         = var.acme_email
    compose_version    = "v5.6.1"
  })

  tags = {
    Name = local.name
  }

  lifecycle {
    # The database lives on this server's disk: never replace it just because a newer
    # Amazon Linux image came out or the first-boot script was edited.
    ignore_changes = [ami, user_data]
  }
}

# A fixed public IP, so DNS keeps pointing at the server across stops and starts.
resource "aws_eip" "server" {
  instance = aws_instance.server.id
  domain   = "vpc"
  tags = {
    Name = local.name
  }
}

data "aws_route53_zone" "main" {
  name = var.hosted_zone_name
}

# food.<your domain> -> the server. Your games site's records are untouched.
resource "aws_route53_record" "app" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = local.site_domain
  type    = "A"
  ttl     = 300
  records = [aws_eip.server.public_ip]
}
