# GitHub Actions CD to Google Kubernetes Engine

This project deploys to GKE from GitHub Actions without a Google JSON key.

The workflow is `.github/workflows/gke-cd.yml`.

It does this on every push to `main`:

1. Authenticates to Google Cloud using GitHub OIDC and Workload Identity Federation.
2. Builds the Docker image from `Dockerfile`.
3. Pushes the image to Artifact Registry using the short commit SHA and `latest` tags.
4. Gets GKE credentials.
5. Applies Kubernetes manifests from `k8s/`.
6. Rolls out the exact image tag to the backend Deployment.

## GitHub repository variables

Go to:

`GitHub repo -> Settings -> Secrets and variables -> Actions -> Variables`

Add these repository variables:

| Variable | Example |
| --- | --- |
| `GCP_PROJECT_ID` | `project-a3605d87-a822-4c2a-b51` |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/github-actions/providers/github` |
| `GCP_SERVICE_ACCOUNT_EMAIL` | `github-cd@project-a3605d87-a822-4c2a-b51.iam.gserviceaccount.com` |
| `GCP_REGION` | `africa-south1` |
| `GKE_LOCATION` | `africa-south1` |
| `GKE_CLUSTER_NAME` | `king-sparkon-gke` |
| `ARTIFACT_REGISTRY_REPO` | `king-sparkon-tracker` |
| `IMAGE_NAME` | `king-sparkon-tracker-backend` |
| `K8S_NAMESPACE` | `king-sparkon` |
| `FRONTEND_DOMAIN` | `https://YOUR_FRONTEND_DOMAIN` |

No Google JSON key is required.

## One-time Google Cloud setup

Run this from Cloud Shell.

```bash
export PROJECT_ID="project-a3605d87-a822-4c2a-b51"
export REGION="africa-south1"
export CLUSTER_NAME="king-sparkon-gke"
export AR_REPO="king-sparkon-tracker"
export REPO_FULL_NAME="leonard1thecoder/king-sparkon-tracker-backend"
export K8S_NAMESPACE="king-sparkon"

export PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
export WIF_POOL_ID="github-actions"
export WIF_PROVIDER_ID="github"
export CD_GSA_NAME="github-cd"
export CD_GSA_EMAIL="$CD_GSA_NAME@$PROJECT_ID.iam.gserviceaccount.com"
export RUNTIME_GSA_NAME="kst-backend-gsa"
export RUNTIME_GSA_EMAIL="$RUNTIME_GSA_NAME@$PROJECT_ID.iam.gserviceaccount.com"
export BUCKET_NAME="king-sparkon-tracker-images"
```

Enable APIs:

```bash
gcloud config set project "$PROJECT_ID"

gcloud services enable \
  container.googleapis.com \
  artifactregistry.googleapis.com \
  iamcredentials.googleapis.com \
  storage.googleapis.com \
  cloudresourcemanager.googleapis.com
```

Create Artifact Registry if it does not already exist:

```bash
gcloud artifacts repositories describe "$AR_REPO" --location="$REGION" >/dev/null 2>&1 || \
  gcloud artifacts repositories create "$AR_REPO" \
    --repository-format=docker \
    --location="$REGION" \
    --description="King Sparkon Tracker Docker images"
```

Create GKE Autopilot cluster if it does not already exist:

```bash
gcloud container clusters describe "$CLUSTER_NAME" --location="$REGION" >/dev/null 2>&1 || \
  gcloud container clusters create-auto "$CLUSTER_NAME" \
    --location="$REGION" \
    --release-channel=regular

gcloud container clusters get-credentials "$CLUSTER_NAME" \
  --location="$REGION" \
  --project="$PROJECT_ID"
```

Create the image bucket and runtime service account:

```bash
gcloud storage buckets describe "gs://$BUCKET_NAME" >/dev/null 2>&1 || \
  gcloud storage buckets create "gs://$BUCKET_NAME" \
    --location="$REGION" \
    --uniform-bucket-level-access

gcloud iam service-accounts describe "$RUNTIME_GSA_EMAIL" >/dev/null 2>&1 || \
  gcloud iam service-accounts create "$RUNTIME_GSA_NAME" \
    --display-name="King Sparkon Tracker Backend GKE Runtime"

gcloud storage buckets add-iam-policy-binding "gs://$BUCKET_NAME" \
  --member="serviceAccount:$RUNTIME_GSA_EMAIL" \
  --role="roles/storage.objectAdmin"
```

Create the CD service account:

```bash
gcloud iam service-accounts describe "$CD_GSA_EMAIL" >/dev/null 2>&1 || \
  gcloud iam service-accounts create "$CD_GSA_NAME" \
    --display-name="King Sparkon Tracker GitHub CD"
```

Grant CD permissions:

```bash
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$CD_GSA_EMAIL" \
  --role="roles/artifactregistry.writer"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$CD_GSA_EMAIL" \
  --role="roles/container.developer"
```

Create Workload Identity pool and provider:

```bash
gcloud iam workload-identity-pools describe "$WIF_POOL_ID" --location="global" >/dev/null 2>&1 || \
  gcloud iam workload-identity-pools create "$WIF_POOL_ID" \
    --project="$PROJECT_ID" \
    --location="global" \
    --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers describe "$WIF_PROVIDER_ID" \
  --location="global" \
  --workload-identity-pool="$WIF_POOL_ID" >/dev/null 2>&1 || \
  gcloud iam workload-identity-pools providers create-oidc "$WIF_PROVIDER_ID" \
    --project="$PROJECT_ID" \
    --location="global" \
    --workload-identity-pool="$WIF_POOL_ID" \
    --display-name="GitHub" \
    --issuer-uri="https://token.actions.githubusercontent.com" \
    --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner,attribute.ref=assertion.ref" \
    --attribute-condition="assertion.repository=='$REPO_FULL_NAME' && assertion.ref=='refs/heads/main'"
```

Allow GitHub Actions to impersonate the CD service account:

```bash
gcloud iam service-accounts add-iam-policy-binding "$CD_GSA_EMAIL" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$WIF_POOL_ID/attribute.repository/$REPO_FULL_NAME"
```

Allow the Kubernetes backend service account to use the runtime Google service account:

```bash
gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_GSA_EMAIL" \
  --role="roles/iam.workloadIdentityUser" \
  --member="serviceAccount:$PROJECT_ID.svc.id.goog[$K8S_NAMESPACE/backend]"
```

Create Kubernetes namespace and runtime secret:

```bash
kubectl apply -f k8s/namespace.yaml

kubectl -n "$K8S_NAMESPACE" create secret generic king-sparkon-backend-secrets \
  --from-literal=SUPABASE_DB_URL="jdbc:postgresql://YOUR_DB_HOST:6543/postgres?prepareThreshold=0" \
  --from-literal=SUPABASE_DB_USER="postgres.YOUR_PROJECT_REF" \
  --from-literal=SUPABASE_DB_PASSWORD="YOUR_DB_PASSWORD" \
  --from-literal=JWT_SECRET="YOUR_LONG_RANDOM_JWT_SECRET" \
  --from-literal=STRIPE_SECRET_KEY="YOUR_STRIPE_SECRET_KEY" \
  --from-literal=STRIPE_WEBHOOK_SECRET="YOUR_STRIPE_WEBHOOK_SECRET" \
  --from-literal=PAYPAL_CLIENT_SECRET="YOUR_PAYPAL_CLIENT_SECRET" \
  --from-literal=PAYPAL_WEBHOOK_ID="YOUR_PAYPAL_WEBHOOK_ID" \
  --from-literal=MAIL_PASSWORD="YOUR_MAIL_PASSWORD" \
  --from-literal=TWILIO_AUTH_TOKEN="YOUR_TWILIO_AUTH_TOKEN" \
  --from-literal=SPRING_DATA_REDIS_PASSWORD="YOUR_REDIS_PASSWORD" \
  --dry-run=client -o yaml | kubectl apply -f -
```

Give the CD service account Kubernetes deploy access:

```bash
kubectl create clusterrolebinding github-cd-cluster-admin \
  --clusterrole=cluster-admin \
  --user="$CD_GSA_EMAIL" \
  --dry-run=client -o yaml | kubectl apply -f -
```

For a stricter production setup, replace `cluster-admin` with a namespace-scoped Role that can manage Deployments, Services, HPAs, ConfigMaps, ServiceAccounts, Pods, and Events in `king-sparkon`.

## GitHub variable values

After setup, set this exact provider value in GitHub Actions variables:

```bash
echo "projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/$WIF_POOL_ID/providers/$WIF_PROVIDER_ID"
```

Set:

```text
GCP_WORKLOAD_IDENTITY_PROVIDER=projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/github-actions/providers/github
GCP_SERVICE_ACCOUNT_EMAIL=github-cd@project-a3605d87-a822-4c2a-b51.iam.gserviceaccount.com
```

## Run deployment

Deployment runs automatically on push to `main`.

To run manually:

1. Go to GitHub repo.
2. Open Actions.
3. Select `GKE CD`.
4. Click `Run workflow`.
5. Optionally enter an image tag.

## Verify deployment

```bash
kubectl -n king-sparkon rollout status deployment/king-sparkon-tracker-backend
kubectl -n king-sparkon get pods
kubectl -n king-sparkon get service king-sparkon-tracker-backend
```

When the service gets an external IP:

```bash
export BACKEND_IP="YOUR_EXTERNAL_IP"
curl "http://$BACKEND_IP/actuator/health"
```

## Test image upload

```bash
curl -X POST "http://$BACKEND_IP/api/v1/pictures" \
  -H "Authorization: Bearer YOUR_JWT" \
  -F "picture=@./poster.png" \
  -F "folder=tickets/posters"
```

The response should contain a Google Storage URL.
