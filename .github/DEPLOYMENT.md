# Gateway CI/CD Setup

## GitHub Environment Configuration

1. Go to repository **Settings** → **Environments**
2. Create environment: `dev`
3. Add variable:
   - `GITHUB_ROLE_ARN` = `arn:aws:iam::579540613167:role/dev-github-actions-role`

## Deployment

Pushes to `dev` branch trigger automatic deployment:
- Build Docker image
- Push to ECR: `paragrafic/gateway`
- Deploy to ECS: `dev-cluster/dev-gateway`

Manual trigger: Actions tab → Deploy to Dev → Run workflow
