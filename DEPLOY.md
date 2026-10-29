# Deploying Food Passport to AWS

## The shape of it

```
   food.<your-domain>  (Route 53 A record)
            │
            ▼
   Elastic IP ──► EC2 t3.micro (Amazon Linux 2023, Docker)
                  ├── caddy     HTTPS (Let's Encrypt, auto-renewing) → app
                  ├── app       Spring Boot: the API *and* the website (one image)
                  └── postgres  data in a Docker volume, pg_dump to S3 nightly
                         │
          ECR (images) ◄─┼─► S3 photos bucket       S3 ops bucket (deploy files, backups)

   GitHub Actions, on every push to main:
     test → build image → push to ECR → upload deploy/ to S3 → SSM "run deploy.sh <sha>" → smoke test
```

Why this way:

- **One small server** is the cheapest real setup. Managed services (RDS, Fargate, a load balancer)
  would be ~$40+/month for the same app.
- **No SSH, no AWS keys in GitHub.** GitHub proves who it is with a short-lived OIDC token, and the
  server takes commands through AWS Systems Manager. Port 22 is closed.
- **Everything is Terraform** (`infra/terraform`), so it can be reviewed, rebuilt or deleted with one command.

## Cost (approximate)

| Item | Free tier | After |
|---|---|---|
| EC2 t3.micro | free-tier eligible* | ~$7.60/month |
| Public IPv4 address | AWS charges for these | ~$3.60/month |
| EBS 20 GB gp3 | free-tier eligible* | ~$1.60/month |
| S3, ECR, SSM, Route 53 record | pennies | pennies |

\* What "free tier" means depends on when your AWS account was created (12-month free tier, or
credits on newer accounts). Your billing alerts will tell you what's real.

## One-time setup

You need: the AWS CLI logged in to your account (`aws sts get-caller-identity` works) and
Terraform 1.6+.

1. **Configure**
   ```bash
   cd infra/terraform
   cp terraform.tfvars.example terraform.tfvars   # then edit: region, domain, subdomain, email
   ```
   If your account already has a GitHub OIDC provider (IAM → Identity providers →
   `token.actions.githubusercontent.com`), set `create_github_oidc_provider = false`.

2. **Create the infrastructure**
   ```bash
   terraform init
   terraform plan    # read it: it should only ADD resources, and touch nothing of your games site
   terraform apply
   ```
   `terraform.tfstate` now contains the generated secrets. It is git-ignored; keep it safe
   (or move state to an S3 backend later).

3. **Tell GitHub where to deploy.** `terraform output github_actions_variables` prints six values.
   In GitHub: repo **Settings → Secrets and variables → Actions → Variables**, add each one.
   Then **Settings → Environments → New environment** called `production`, and under
   **Deployment branches and tags** choose **Selected branches** → add `main`. AWS only trusts jobs
   in this environment, so this is what stops any other branch from deploying. (You can also
   require your approval for each deploy here.)

4. **First deploy.** Merge to `main`, or open **Actions → CI/CD → Run workflow** on `main`.
   Caddy fetches the HTTPS certificate on first start, so give it a minute.
   Then open `https://food.<your-domain>`.

## Day to day

- **Deploying** = merging to `main`. Pull requests only run the tests.
- **Logs**: open a shell on the server without SSH:
  ```bash
  aws ssm start-session --target <SERVER_INSTANCE_ID>
  sudo docker logs --tail 100 foodpassport-app-1
  ```
  (needs the [Session Manager plugin](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html))
- **Backups** run nightly and are kept 30 days: `aws s3 ls s3://<OPS_BUCKET>/backups/`.
  Run one now: `sudo systemctl start foodpassport-backup`.
- **Restore** a backup (on the server):
  ```bash
  aws s3 cp s3://<OPS_BUCKET>/backups/<file>.sql.gz - | gunzip \
    | sudo docker exec -i foodpassport-postgres-1 psql -U foodpassport foodpassport
  ```

## Known limits (fine for friends, worth knowing)

- One server: a deploy restarts the app (a few seconds of downtime), and if the server dies you
  restore from the latest backup.
- No email verification or login rate limiting yet. Add those before opening it to the public.
- The server doesn't install OS updates by itself: `sudo dnf upgrade --releasever=latest` now and then.

## Tearing it down

`terraform destroy` removes everything it created. **It deletes the database with the server**,
so download a backup first. S3 buckets that still contain files are not deleted (on purpose).
