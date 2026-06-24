# King Sparkon Tracker Backend

Spring Boot backend for King Sparkon Tracker: barcode stock control, business tenants, owner and worker access, inventory transactions, reports, billing, tips, payouts, and audit logs.

This repository is hardened for Google Cloud Run, PostgreSQL, Redis-backed rate limiting, Stripe website payments, PayPal billing and payouts, Flyway migrations, Actuator metrics, Prometheus, and Grafana.

## Production Hardening Added

| Area | Result |
| --- | --- |
| Refresh-token flow | Added refresh token entity, repository, rotation service, login response fields, refresh endpoint, logout endpoint, and password-reset revocation. |
| Error codes | Added stable machine-readable error codes through `ErrorCode` and the API exception handler. |
| Database | Added Flyway migration for refresh token persistence. |
| Cloud Run | Added Dockerfile. |
| CI | Added GitHub Actions Maven verify and Docker build workflow. |
| Observability | Added Actuator, Prometheus registry dependency, and Grafana dashboard JSON. |
| Startup safety | Added production startup configuration validator. |
| Tests | Added focused tests for production config, stock locking, and webhook idempotency mapping. |
| Tip fee | Confirmed worker tip withdrawal fee is 8.5%. |

## System Architecture

```mermaid
flowchart LR
    UI[Next.js Web App] --> API[Cloud Run Spring Boot API]
    API --> DB[(PostgreSQL)]
    API --> REDIS[(Redis or Memorystore)]
    API --> STRIPE[Stripe]
    API --> PAYPAL[PayPal]
    API --> SMTP[Email Provider]
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
    participant Stripe

    Owner->>API: Register business
    API->>DB: Create tenant and owner
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
        Stripe->>API: Signed webhook
        API->>DB: Mark payment paid once
    else Cash or swipe
        API->>DB: Save offline payment state
    end
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

## Use Cases

```mermaid
flowchart TB
    Owner((Owner))
    Worker((Worker))
    Customer((Customer))
    Stripe((Stripe))
    PayPal((PayPal))

    Owner --> RegisterBusiness[Register business]
    Owner --> CreateWorker[Create worker]
    Owner --> CreateProduct[Create product]
    Owner --> ViewReports[View reports]
    Owner --> RequestWithdrawal[Request tip withdrawal]
    Worker --> Login[Login]
    Worker --> AssignBarcode[Assign product barcode]
    Worker --> CreateTransaction[Create BUY or SELL transaction]
    Worker --> CreateTip[Create tip payment link]
    Customer --> CreateTransaction
    Customer --> CreateTip
    Stripe --> StripeWebhook[Send signed payment webhook]
    PayPal --> PayPalWebhook[Send verified billing webhook]
```

## UML: Refresh Token Flow

```mermaid
classDiagram
    class AuthenticationController {
      +login(LoginRequest) AuthResponse
      +refresh(RefreshTokenRequest) AuthResponse
      +logout(RefreshTokenRequest) MessageResponse
    }
    class RefreshTokenService {
      +issueTokenPair(TrackerUser, String, String) TokenPair
      +rotate(String, String, String) TokenPair
      +revoke(String) void
      +revokeAllForUser(Long) void
    }
    class RefreshTokenRepository
    class RefreshToken
    AuthenticationController --> RefreshTokenService
    RefreshTokenService --> RefreshTokenRepository
    RefreshTokenRepository --> RefreshToken
```

## UML: Stock Movement

```mermaid
classDiagram
    class TransactionService {
      +createTransaction(CreateTransactionRequest, String) InventoryTransaction
    }
    class ProductService {
      +getProductForStockUpdate(Long, Long) Product
      +applyStockMovement(Product, TransactionType, int) Product
    }
    class ProductRepository {
      +findLockedByIdAndBusiness_Id(Long, Long) Optional~Product~
    }
    TransactionService --> ProductService
    ProductService --> ProductRepository
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

## Running Tests and Coverage

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
| Tips | 8.5% fee, 7-day hold, R1000 minimum, owner-only withdrawal. |
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

## Observability

Grafana dashboard:

```text
ops/grafana/king-sparkon-cloud-run-dashboard.json
```

Recommended panels: HTTP request volume, 5xx rate, P95 latency, JVM memory, CPU, database connections, and 429 responses.

## Business Rules Worth Protecting

- Every business resource must be scoped to the authenticated business.
- Product barcodes must be unique.
- Product stock cannot go below zero.
- SELL transactions require scanned barcodes.
- Website payments are only marked paid by verified webhooks.
- Webhook event ids must be processed idempotently.
- Tip withdrawal fee is 8.5%.
- Sensitive actions must be written to audit logs.
