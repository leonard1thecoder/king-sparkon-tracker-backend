# King Sparkon Tracker Backend on Google Kubernetes Engine

This folder deploys the backend Docker image to Google Kubernetes Engine (GKE).

## Files

- `namespace.yaml` - `king-sparkon` namespace.
- `service-account.yaml` - Kubernetes ServiceAccount mapped to a Google IAM service account.
- `configmap.yaml` - non-secret runtime configuration.
- `secret.example.yaml` - placeholder secret template. Do not commit real values.
- `deployment.yaml` - backend Deployment with rolling updates, probes, and resource limits.
- `service.yaml` - external LoadBalancer service.
- `hpa.yaml` - CPU-based autoscaling.
- `kustomization.yaml` - applies all resources and lets you override the Docker image tag.

## Recommended variables

```bash
export PROJECT_ID="project-a3605d87-a822-4c2a-b51"
export REGION="africa-south1"
export CLUSTER_NAME="king-sparkon-gke"
export AR_REPO="king-sparkon-tracker"
export IMAGE_NAME="king-sparkon-tracker-backend"
export IMAGE_TAG="$(git rev-parse --short HEAD)"
export IMAGE_URI="$REGION-docker.pkg.dev/$PROJECT_ID/$AR_REPO/$IMAGE_NAME:$IMAGE_TAG"
export BUCKET_NAME="king-sparkon-tracker-images"
export GSA_NAME="kst-backend-gsa"
export GSA_EMAIL="$GSA_NAME@$PROJECT_ID.iam.gserviceaccount.com"
```

## Enable Google Cloud services

```bash
gcloud config set project "$PROJECT_ID"

gcloud services enable \
  container.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  iamcredentials.googleapis.com \
  storage.googleapis.com
```

## Create Artifact Registry and build the Docker image

```bash
gcloud artifacts repositories create "$AR_REPO" \
  --repository-format=docker \
  --location="$REGION" \
  --description="King Sparkon Tracker Docker images"

gcloud builds submit --tag "$IMAGE_URI" .
```

## Create a GKE Autopilot cluster

```bash
gcloud container clusters create-auto "$CLUSTER_NAME" \
  --location="$REGION" \
  --release-channel=regular

gcloud container clusters get-credentials "$CLUSTER_NAME" \
  --location="$REGION" \
  --project="$PROJECT_ID"
```

## Create Google Storage bucket and IAM service account

```bash
gcloud storage buckets create "gs://$BUCKET_NAME" \
  --location="$REGION" \
  --uniform-bucket-level-access

gcloud iam service-accounts create "$GSA_NAME" \
  --display-name="King Sparkon Tracker Backend GKE"

gcloud storage buckets add-iam-policy-binding "gs://$BUCKET_NAME" \
  --member="serviceAccount:$GSA_EMAIL" \
  --role="roles/storage.objectAdmin"
```

## Link Kubernetes ServiceAccount to Google IAM

Replace `PROJECT_ID` in `service-account.yaml` before applying:

```bash
sed -i "s/PROJECT_ID/$PROJECT_ID/g" k8s/service-account.yaml
sed -i "s/PROJECT_ID/$PROJECT_ID/g" k8s/configmap.yaml
sed -i "s/PROJECT_ID/$PROJECT_ID/g" k8s/deployment.yaml
sed -i "s/PROJECT_ID/$PROJECT_ID/g" k8s/kustomization.yaml
```

Allow the Kubernetes ServiceAccount to use the Google IAM service account:

```bash
gcloud iam service-accounts add-iam-policy-binding "$GSA_EMAIL" \
  --role="roles/iam.workloadIdentityUser" \
  --member="serviceAccount:$PROJECT_ID.svc.id.goog[king-sparkon/backend]"
```

## Create Kubernetes secrets

Do not apply `secret.example.yaml` with placeholder values in production. Create a real secret like this:

```bash
kubectl apply -f k8s/namespace.yaml

kubectl -n king-sparkon create secret generic king-sparkon-backend-secrets \
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

## Deploy to GKE

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/service-account.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml

kubectl -n king-sparkon set image deployment/king-sparkon-tracker-backend \
  backend="$IMAGE_URI"

kubectl -n king-sparkon rollout status deployment/king-sparkon-tracker-backend
kubectl -n king-sparkon get pods
kubectl -n king-sparkon get service king-sparkon-tracker-backend
```

## Smoke test

After the service receives an external IP address:

```bash
export BACKEND_IP="YOUR_EXTERNAL_IP"
curl "http://$BACKEND_IP/actuator/health"
```

## Image upload test

```bash
curl -X POST "http://$BACKEND_IP/api/v1/pictures" \
  -H "Authorization: Bearer YOUR_JWT" \
  -F "picture=@./poster.png" \
  -F "folder=tickets/posters"
```

The response should return a Google Storage URL. Save that URL in the app fields like `profilePictureUrl`, `bannerUrl`, or `posterPhotoUrl`.
