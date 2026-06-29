#!/usr/bin/env bash
set -euo pipefail

failures=0

fail() {
  echo "::error::$1"
  failures=$((failures + 1))
}

pass() {
  echo "✅ $1"
}

require_file() {
  local file="$1"
  if [[ ! -f "$file" ]]; then
    fail "Missing required file: $file"
  else
    pass "Found $file"
  fi
}

require_contains() {
  local file="$1"
  local expected="$2"
  local label="$3"
  if grep -Fq "$expected" "$file"; then
    pass "$label"
  else
    fail "$label is missing from $file"
  fi
}

require_not_contains() {
  local file="$1"
  local blocked="$2"
  local label="$3"
  if grep -Fq "$blocked" "$file"; then
    fail "$label found in $file"
  else
    pass "$label not found"
  fi
}

require_file "k8s/namespace.yaml"
require_file "k8s/service-account.yaml"
require_file "k8s/configmap.yaml"
require_file "k8s/secret.example.yaml"
require_file "k8s/deployment.yaml"
require_file "k8s/service.yaml"
require_file "k8s/hpa.yaml"
require_file "k8s/kustomization.yaml"
require_file ".github/workflows/gke-cd.yml"

require_contains "k8s/configmap.yaml" 'APP_GOOGLE_STORAGE_ENABLED: "true"' "Google Storage enabled"
require_contains "k8s/configmap.yaml" 'APP_GOOGLE_STORAGE_PROJECT_ID: "project-a3605d87-a822-4c2a-b51"' "Google Storage project id configured"
require_contains "k8s/configmap.yaml" 'APP_GOOGLE_STORAGE_BUCKET_NAME: "king-sparkon-tracker-images"' "Google Storage bucket configured"
require_contains "k8s/configmap.yaml" 'APP_GOOGLE_STORAGE_PUBLIC_BASE_URL: "https://storage.googleapis.com/king-sparkon-tracker-images"' "Google Storage public URL configured"
require_contains "k8s/configmap.yaml" 'APP_GOOGLE_STORAGE_ROOT_FOLDER: "king-sparkon-tracker"' "Google Storage root folder configured"
require_contains "k8s/configmap.yaml" 'APP_GOOGLE_STORAGE_MAKE_PUBLIC: "false"' "Google Storage public ACL disabled"
require_contains "k8s/configmap.yaml" 'APP_GOOGLE_STORAGE_REJECT_EXTERNAL_IMAGE_URLS: "true"' "External image URLs rejected"
require_contains "k8s/configmap.yaml" 'APP_GOOGLE_STORAGE_MAX_FILE_SIZE_BYTES: "5242880"' "Google Storage image size limit configured"

require_not_contains "k8s/configmap.yaml" 'APP_GOOGLE_STORAGE_PROJECT_ID: "PROJECT_ID"' "Unrendered Google Storage project placeholder"
require_not_contains "k8s/kustomization.yaml" 'secret.example.yaml' "Secret example in kustomization resources"

# Active GKE/CD runtime files must not require a Google service-account JSON path.
# Documentation may mention GOOGLE_APPLICATION_CREDENTIALS for local laptop testing only.
if grep -R "GOOGLE_APPLICATION_CREDENTIALS" k8s .github/workflows --exclude='validate-k8s-env.sh' >/dev/null 2>&1; then
  fail "GOOGLE_APPLICATION_CREDENTIALS must not be required in active GKE/CD files; use Workload Identity"
else
  pass "No Google JSON credential path required in active GKE/CD files"
fi

for secret_name in \
  SUPABASE_DB_URL \
  SUPABASE_DB_USER \
  SUPABASE_DB_PASSWORD \
  JWT_SECRET \
  STRIPE_SECRET_KEY \
  STRIPE_WEBHOOK_SECRET \
  PAYPAL_CLIENT_SECRET \
  PAYPAL_WEBHOOK_ID \
  MAIL_PASSWORD \
  TWILIO_AUTH_TOKEN \
  SPRING_DATA_REDIS_PASSWORD; do
  require_contains "k8s/secret.example.yaml" "$secret_name" "Secret template contains $secret_name"
done

require_contains ".github/workflows/gke-cd.yml" 'id-token: write' "GitHub OIDC permission configured"
require_contains ".github/workflows/gke-cd.yml" 'google-github-actions/auth@v3' "Google auth action configured"
require_contains ".github/workflows/gke-cd.yml" 'GCP_WORKLOAD_IDENTITY_PROVIDER' "Workload Identity provider variable checked"
require_contains ".github/workflows/gke-cd.yml" 'GCP_SERVICE_ACCOUNT_EMAIL' "CD service account variable checked"
require_contains ".github/workflows/gke-cd.yml" 'king-sparkon-backend-secrets' "Runtime secret existence checked"
require_contains ".github/workflows/gke-cd.yml" 'rollout status deployment/king-sparkon-tracker-backend' "Rollout status check configured"

if [[ "$failures" -gt 0 ]]; then
  echo "❌ Environment validation failed with $failures issue(s)."
  exit 1
fi

echo "✅ Environment validation passed."
