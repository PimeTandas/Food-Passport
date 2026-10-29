#!/bin/bash
# Runs ON the server (sent by GitHub Actions through AWS Systems Manager).
# Usage: deploy.sh <image-tag>
set -euo pipefail

TAG="${1:?usage: deploy.sh <image-tag>}"
HERE="$(cd "$(dirname "$0")" && pwd)"
# Written once by the server's first-boot script: AWS_REGION, ECR_REPOSITORY_URL, PHOTOS_BUCKET,
# OPS_BUCKET, PARAM_PREFIX, SITE_DOMAIN, ACME_EMAIL
source /opt/foodpassport/host.env

param() {
  aws ssm get-parameter --region "$AWS_REGION" --with-decryption --name "$PARAM_PREFIX/$1" \
    --query Parameter.Value --output text
}

# 1. Secrets from SSM Parameter Store -> .env (readable by root only)
umask 077
cat > "$HERE/.env" <<ENV
APP_IMAGE=$ECR_REPOSITORY_URL:$TAG
DB_PASSWORD=$(param db-password)
JWT_SECRET=$(param jwt-secret)
PHOTOS_BUCKET=$PHOTOS_BUCKET
AWS_REGION=$AWS_REGION
SITE_DOMAIN=$SITE_DOMAIN
ACME_EMAIL=$ACME_EMAIL
ENV

# 2. Log in to our private image registry (ECR) using the server's IAM role
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "${ECR_REPOSITORY_URL%%/*}"

# 3. Pull the new image and (re)start whatever changed
cd "$HERE"
docker compose -f compose.prod.yaml --env-file .env pull --quiet
docker compose -f compose.prod.yaml --env-file .env up -d --remove-orphans

# 4. Wait until Spring reports healthy, or fail the deploy
for i in $(seq 1 60); do
  status="$(docker inspect -f '{{.State.Health.Status}}' foodpassport-app-1 2>/dev/null || echo starting)"
  if [ "$status" = "healthy" ]; then
    echo "Deployed $TAG: app is healthy"
    docker image prune -f >/dev/null
    exit 0
  fi
  sleep 5
done
echo "App did not become healthy. Last logs:" >&2
docker logs --tail 80 foodpassport-app-1 >&2 || true
exit 1
