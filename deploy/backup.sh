#!/bin/bash
# Nightly database backup (run by a systemd timer on the server): pg_dump -> gzip -> S3.
# S3 keeps backups for 30 days (lifecycle rule in Terraform).
set -euo pipefail
source /opt/foodpassport/host.env

name="backups/foodpassport-$(date -u +%Y-%m-%dT%H%M%SZ).sql.gz"
docker exec foodpassport-postgres-1 pg_dump -U foodpassport --no-owner foodpassport \
  | gzip \
  | aws s3 cp - "s3://$OPS_BUCKET/$name" --region "$AWS_REGION"
echo "Backed up to s3://$OPS_BUCKET/$name"
