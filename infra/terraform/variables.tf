variable "region" {
  description = "AWS region to run in, e.g. eu-west-2 (London) or eu-west-1 (Ireland)."
  type        = string
}

variable "hosted_zone_name" {
  description = "Your existing Route 53 hosted zone, e.g. example.com."
  type        = string
}

variable "subdomain" {
  description = "The app lives at <subdomain>.<hosted_zone_name>."
  type        = string
  default     = "food"
}

variable "acme_email" {
  description = "Email Let's Encrypt uses to warn you about certificate problems."
  type        = string
}

variable "github_repository" {
  description = "owner/repo that is allowed to deploy. Case-sensitive."
  type        = string
  default     = "PimeTandas/Food-Passport"
}

variable "instance_type" {
  description = "t3.micro is free-tier eligible (1 GB RAM)."
  type        = string
  default     = "t3.micro"
}

variable "create_github_oidc_provider" {
  description = "An AWS account can only have one GitHub OIDC provider. Set false if you already have one (e.g. for your games site)."
  type        = bool
  default     = true
}
