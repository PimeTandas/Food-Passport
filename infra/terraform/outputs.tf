output "site_url" {
  value = "https://${local.site_domain}"
}

output "server_public_ip" {
  value = aws_eip.server.public_ip
}

# Copy these into GitHub: repo Settings > Secrets and variables > Actions > Variables.
output "github_actions_variables" {
  description = "Repository variables the deploy workflow needs (none of these are secret)."
  value = {
    AWS_REGION          = var.region
    AWS_DEPLOY_ROLE_ARN = aws_iam_role.deploy.arn
    ECR_REPOSITORY_URL  = aws_ecr_repository.app.repository_url
    OPS_BUCKET          = aws_s3_bucket.ops.bucket
    SERVER_INSTANCE_ID  = aws_instance.server.id
    SITE_DOMAIN         = local.site_domain
  }
}
