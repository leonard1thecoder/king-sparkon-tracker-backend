# King Sparkon Tracker Backend

Spring Boot backend for King Sparkon Tracker: barcode stock control, business tenants, owner and worker access, inventory transactions, reports, billing, tips, payouts, subscribers, promotions, affiliate programs, and audit logs.

This repository is hardened for Google Cloud Run, PostgreSQL, Redis caching, Stripe website payments, PayPal billing and payouts, Flyway migrations, Actuator metrics, Prometheus, and Grafana.

Full backend/frontend system contract: [`docs/backend-system-design.md`](docs/backend-system-design.md)

## Production Hardening Added

| Area | Result |
| --- | --- |
| Refresh-token flow | Added refresh token entity, repository, rotation service, login response fields, refresh endpoint, logout endpoint, and password-reset revocation. |
| Error codes | Added stable machine-readable error codes through `ErrorCode` and the API exception handler. |
| Database | Added Flyway migrations and `ddl-auto=validate`. |
| Cloud Run | Added Dockerfile. |
| Build gate | Docker build runs full Maven verification through `scripts/full-maven-scan.sh`. |
| Observability | Added Actuator, Prometheus registry dependency, and Grafana dashboard JSON. |
| Startup safety | Added production startup configuration validator. |
| Redis | Added Redis-backed Spring Cache for roles, billing policy, feature access, and promotion quote data. |
| Tests | Added focused positive/negative tests for production config, authorization, stock locking, webhook idempotency, owner business description, payment contact capture, and caching policy. |
| Tip fee | Worker tip withdrawal fee remains the configured `app.tips.withdrawal-fee-percent`; the removed 2.03% addition is not part of production logic. |

## System Architecture

```mermaid
flowchart LR
    UI[Next.js Web App] --> API[Cloud Run Spring Boot API]
    API --> DB[(PostgreSQL)]
    API --> REDIS[(Redis or Memorystore)]
    API --> STRIPE[Stripe]
    API --> PAYPAL[PayPal]
    API --> SMTP[Email Provider]
    API --> TWILIO[Twilio WhatsApp]
    STRIPE --> API
    PAYPAL --> API
    API --> PROM[Prometheus]
    PROM --> GRAFANA[Grafana]
```

## Core Flow

```mermaid
sequenceDiagram
    actor Owner
    actor Worker
    actor Customer
    participant API as Backend API
    participant DB as PostgreSQL
    participant Redis
    participant Stripe

    Owner->>API: Register business + description
    API->>DB: Create tenant and owner
    API->>DB: Persist business description
    Worker->>API: Login
    API-->>Worker: Access token and refresh token
    Owner->>API: Create product
    Worker->>API: Assign barcodes
    Customer->>Worker: Buys product
    Worker->>API: Create SELL transaction
    API->>DB: Lock product row
    API->>DB: Reduce stock and mark barcodes sold
    alt Website payment
        API->>Stripe: Create payment link
        API->>DB: Persist paymentContact
        API->>DB: Subscribe paymentContact as CLIENT
        Stripe->>API: Signed webhook
        API->>DB: Mark payment paid once
    else Cash or swipe
        API->>DB: Save offline payment state
    end
    API->>Redis: Cache safe policy/reference data
```

## UI-Ready System Design Contract

The frontend should be designed around these screens, roles, endpoints, and business rules.

### Roles and UI Areas

| Role | UI area | Main capabilities |
| --- | --- | --- |
| `Owner` | Owner dashboard | Business onboarding, workers, products, barcodes, transactions, promotions, tips, withdrawals, billing, reports, audit logs. |
| `Worker` | Worker scanner/mobile dashboard | Scan barcodes, create transactions, create tip payment links/QR codes. |
| `Affiliate` | Affiliate dashboard | Promote King Sparkon Tracker, view promotion code/QR/link, complete affiliate onboarding. |
| `Admin` | Admin dashboard | Oversee businesses/users and create registered-subscriber platform promotions. |
| Public user | Public website | Subscribe, unsubscribe, register, verify email, request password reset, open affiliate links. |

### Public API for UI

| Screen | Method | Endpoint | Auth |
| --- | --- | --- | --- |
| Owner registration | `POST` | `/api/auth/register` | Public |
| Admin registration | `POST` | `/api/auth/register-admin` | Public, but email must end with `@kingsparkon.com` |
| Affiliate registration | `POST` | `/api/auth/register-affiliate` | Public |
| Login | `POST` | `/api/auth/login` | Public |
| Refresh session | `POST` | `/api/auth/refresh` | Public with refresh token |
| Logout | `POST` | `/api/auth/logout` | Auth/session token |
| Forgot password | `POST` | `/api/auth/forgot-password` | Public |
| Reset password | `POST` | `/api/auth/reset-password` | Public |
| Verify email | `GET` | `/api/auth/verify-email?token=...` | Public |
| Subscribe | `POST` | `/api/subscribers` | Public |
| Unsubscribe | `DELETE` | `/api/subscribers?contact=...` | Public |
| Contact form | `POST` | `/api/contact-inquiries` | Public |

### Owner Registration UI Payload

```json
{
  "username": "owner",
  "emailAddress": "owner@example.com",
  "password": "secret",
  "businessName": "Spark Store",
  "businessDescription": "Barcode-enabled retail store selling beverages and convenience products.",
  "localizationCountry": "SOUTH_AFRICA",
  "physicalAddress": "12 Main Road, Johannesburg",
  "cellphoneNumber": "+27821234567",
  "affiliateCode": "AFF-ALICE-1234"
}
```

Business rules for UI:

- `businessName` is required.
- `businessDescription` is optional but should be shown on the onboarding screen; max 2000 characters.
- If `physicalAddress` and `cellphoneNumber` are provided, onboarding can be marked complete.
- After registration, the UI must show an email-verification pending state.
- If `affiliateCode` is present, the backend links the business to the affiliate.

### Business Feature Rules for UI

Frontend may show locked/upgrade states, but backend is the source of truth.

| Feature | Free Trial | Plus | Pro |
| --- | --- | --- | --- |
| `CREATE_WORKERS` | Yes, max 2 workers | Yes, max 5 workers | Yes, unlimited workers |
| `CREATE_PRODUCTS` | Yes | Yes | Yes |
| `ADD_BARCODES` | Yes | Yes | Yes |
| `SCAN_BARCODES` | Yes | Yes | Yes |
| `WORKER_TIPS_PLATFORM` | Locked | Locked | Enabled |
| `BUSINESS_ANALYSIS_AI` | Locked | Locked | Enabled |
| `WORKER_CLOCKER` | Locked | Locked | Enabled |

Business status rules:

- `TRIAL` and `ACTIVE` businesses can use allowed features.
- `DEACTIVATED` businesses should show an activation/subscription-required state.
- If a feature is locked, show upgrade CTA to Pro where relevant.

### Transaction / Website Payment UI Payload

```json
{
  "type": "SELL",
  "paymentType": "WEBSITE_PAYMENT",
  "paymentEmail": "customer@example.com",
  "paymentContact": "+27821234567",
  "employeeId": 2,
  "ownerId": 1,
  "items": [
    {
      "productId": 10,
      "quantity": 1,
      "barcodes": ["6001234567890"]
    }
  ]
}
```

Business rules for UI:

- `paymentEmail` remains backward-compatible for Stripe email/payment delivery.
- `paymentContact` can be an email or cellphone.
- `paymentContact` is persisted on the transaction response.
- `paymentContact` is auto-subscribed only when `paymentType = WEBSITE_PAYMENT`.
- `CASH` and `SWIPE_MACHINE` must not auto-subscribe the customer.
- UI should display `paymentUrl`, `paymentStatus`, `paymentReference`, `paymentEmail`, and `paymentContact` in transaction details.

### Subscriber UI Payloads

King Sparkon subscriber:

```json
{
  "contact": "client@example.com",
  "subscriberType": "KINGSPARKON_SUBSCRIBER",
  "preferredChannel": "EMAIL"
}
```

Unregistered affiliate lead:

```json
{
  "contact": "+27821234567",
  "subscriberType": "AFFILIATE",
  "affiliateRegistered": false,
  "preferredChannel": "WHATSAPP"
}
```

Registered affiliate subscriber:

```json
{
  "contact": "affiliate@example.com",
  "subscriberType": "AFFILIATE",
  "affiliateRegistered": true,
  "preferredChannel": "EMAIL"
}
```

Subscriber rules:

- Contact can be email or cellphone.
- Emails are normalized lowercase.
- Cellphones must be international format, for example `+27821234567`.
- Tip payment-link clients and website-payment clients become `CLIENT` subscribers.
- Existing inactive subscribers are reactivated.

### Promotions UI Contract

| Screen | Method | Endpoint | Role |
| --- | --- | --- | --- |
| Promotion quote | `GET` | `/api/promotions/quote` | Owner |
| Create owner promotion | `POST` | `/api/promotions` | Owner |
| List owner promotions | `GET` | `/api/promotions` | Owner |
| Admin registered-subscriber promotion | `POST` | `/api/admin/promotions/registered-subscribers` | Admin |

Promotion audience values:

- `ALL_SUBSCRIBERS`
- `REGISTERED_AFFILIATES`
- `UNREGISTERED_AFFILIATES`
- `REGISTERED_SUBSCRIBERS`

Promotion channel values:

- `EMAIL`
- `WHATSAPP`
- `ANY`

Promotion pricing rules:

| Audience size | Platform fee | Subscriber rate |
| --- | ---: | ---: |
| 1-100 | R49 | R0.90 |
| 101-1000 | R49 | R0.65 |
| 1001+ | R49 | R0.45 |

Promotion anti-crowding rule:

- A subscriber must not receive more than one promotion inside a 2-day window.
- UI should not promise immediate delivery to every subscriber; show campaign status and processed counts.

### Tips UI Contract

Tip payment link payload:

```json
{
  "workerId": 12,
  "tipAmount": 50.00,
  "callbackUrl": "https://app.example/tips/complete",
  "clientContact": "client@example.com"
}
```

Rules:

- Worker creates a tip link/QR.
- `clientContact` subscribes the client by default.
- Tip withdrawal uses configured fee only; no extra 2.03% fee.
- Owner manages withdrawal flow from dashboard.

### Redis Caching Design

Redis caching is enabled through Spring Cache and the `redis` profile.

```bash
SPRING_PROFILES_ACTIVE=redis
```

| Cache | TTL | Purpose |
| --- | ---: | --- |
| `privileges` | 12h | Role list for admin/settings/UI. |
| `privilegeByRole` | 12h | Role lookup during auth/user creation. |
| `billingPlans` | 6h | Public pricing plan cards. |
| `affiliateCommissionTiers` | 6h | Affiliate commission UI. |
| `businessPlanPrices` | 6h | Business plan pricing. |
| `businessPlanWorkerLimits` | 6h | Worker limits per plan. |
| `businessFeatureAccess` | 15m | Feature-policy access keyed by business id, plan, status, and feature. |
| `promotionQuotes` | 10m | Bulk promotion quote pricing. |

Caching rules:

- Do not cache whole `Business` or `TrackerUser` entities for authorization.
- Feature access is cached using business id + plan + status + feature, so upgrades/downgrades naturally use a new key.
- Billing plans and static policy data can be cached longer.
- Promotion quotes use short TTL because audience size changes.

Run Redis locally:

```bash
docker compose -f docker-compose.redis.yml up -d
```

Run backend with Redis locally:

```bash
SPRING_PROFILES_ACTIVE=redis REDIS_HOST=localhost ./mvnw spring-boot:run
```

## ERD

```mermaid
erDiagram
    businesses ||--o{ tracker_users : owns
    privileges ||--o{ tracker_users : grants
    businesses ||--o{ products : scopes
    products ||--o{ product_barcodes : has
    businesses ||--o{ inventory_transactions : scopes
    inventory_transactions ||--o{ transaction_items : contains
    transaction_items ||--o{ transaction_item_barcodes : records
    tracker_users ||--o{ refresh_tokens : has
    tracker_users ||--o{ tips : receives
    tips ||--o{ tip_withdrawals : grouped_into
    tracker_users ||--o{ worker_payout_accounts : payout_account
    businesses ||--o{ business_subscriptions : billing
    businesses ||--o{ audit_logs : audit
    businesses ||--o{ billing_audit_logs : billing_audit

    tracker_users {
        bigint id PK
        string username
        string email_address
        bigint privilege_id FK
        bigint business_id FK
    }

    products {
        bigint id PK
        bigint business_id FK
        string name
        string category
        string status
        decimal price
        int stock_quantity
    }

    product_barcodes {
        bigint id PK
        bigint product_id FK
        string barcode UK
        string status
        string availability_status
        string reference_email
    }

    inventory_transactions {
        bigint id PK
        bigint business_id FK
        bigint employee_id FK
        bigint owner_id FK
        string type
        string payment_type
        string payment_status
        string payment_email
        string payment_contact
        decimal total_amount
    }

    refresh_tokens {
        bigint id PK
        bigint user_id FK
        string token_hash UK
        timestamp issued_at
        timestamp expires_at
        timestamp revoked_at
    }
```

## Stable Error Contract

All API failures should expose a stable `code` field so the Next.js UI can show exact states.

Example:

```json
{
  "timestamp": "2026-06-24T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Transaction must contain at least one item",
  "path": "/api/transactions",
  "requestId": "request-id-from-logs"
}
```

Important codes include `VALIDATION_FAILED`, `AUTHENTICATION_FAILED`, `EMAIL_NOT_VERIFIED`, `RESOURCE_NOT_FOUND`, `DUPLICATE_BARCODE`, `INSUFFICIENT_STOCK`, `RATE_LIMIT_EXCEEDED`, `PAYMENT_FAILED`, `WEBHOOK_SIGNATURE_INVALID`, `WEBHOOK_DUPLICATE`, and `BUSINESS_ACCESS_RESTRICTED`.

## Running Locally

```bash
./mvnw spring-boot:run
```

With Redis:

```bash
docker compose -f docker-compose.redis.yml up -d
SPRING_PROFILES_ACTIVE=redis REDIS_HOST=localhost ./mvnw spring-boot:run
```

## Running Tests and Coverage

```bash
./scripts/full-maven-scan.sh
```

Equivalent Maven command:

```bash
./mvnw -B clean verify
```

Coverage output:

```text
target/site/jacoco/index.html
target/site/jacoco/jacoco.xml
```

Minimum production test gates:

| Area | Required tests |
| --- | --- |
| Auth | Login, refresh rotation, logout, password reset revokes refresh tokens. |
| Authorization | Owner-only endpoints reject worker and affiliate roles. |
| Tenant isolation | Users cannot read or mutate another business's resources. |
| Stock | Concurrent SELL attempts cannot oversell locked product rows. |
| Barcodes | Duplicate barcode rejected and sold barcode cannot be reused. |
| Stripe | Signature failure rejected, duplicate event skipped, success event marks payment paid once. |
| PayPal | Verified webhook, duplicate event skipped, billing state transitions. |
| Tips | Configured fee, 7-day hold, R1000 minimum, owner-only withdrawal. |
| Subscribers | Email/cellphone positive paths, invalid phone/email negative paths, unsubscribe/reactivation. |
| Promotions | Audience targeting, 2-day anti-crowding, email/WhatsApp channel handling. |
| Redis cache | Cache names, TTLs, feature-policy key safety. |
| Rate limiting | Public auth limit and tenant-plan limits. |
| Config | Production profile fails startup when required configuration is missing. |

## Cloud Run

Build:

```bash
docker build -t king-sparkon-tracker-backend .
```

Deploy:

```bash
gcloud run deploy king-sparkon-tracker-backend \
  --image REGION-docker.pkg.dev/PROJECT/king-sparkon/backend:TAG \
  --region REGION \
  --platform managed \
  --allow-unauthenticated \
  --port 8080 \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod
```

Production secrets must be stored in Google Secret Manager or Cloud Run environment variables. Do not commit credentials.

## Observability

Grafana dashboard:

```text
ops/grafana/king-sparkon-cloud-run-dashboard.json
```

Recommended panels: HTTP request volume, 5xx rate, P95 latency, JVM memory, CPU, database connections, Redis latency/errors, promotion send failures, webhook failures, and 429 responses.

## Business Rules Worth Protecting

- Every business resource must be scoped to the authenticated business.
- Owner promotions are owner-only.
- Admin APIs are admin-only.
- Subscriber signup/unsubscribe is public.
- Product barcodes must be unique.
- Product stock cannot go below zero.
- SELL transactions require scanned barcodes.
- Website payments are only marked paid by verified webhooks.
- Website-payment contacts are persisted and subscribed only for website payments.
- CASH and SWIPE payments must not auto-subscribe customers.
- Webhook event ids must be processed idempotently.
- Tip withdrawal uses the configured fee only.
- Promotion sends must respect the 2-day anti-crowding rule.
- Sensitive actions must be written to audit logs.
