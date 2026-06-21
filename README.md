# King Sparkon Tracker Backend

King Sparkon Tracker Backend is a Spring Boot API for managing barcoded product stock, worker activity, owner oversight, transaction history, inventory reporting, and subscription billing across multiple businesses. Each owner creates a business tenant, controls that tenant's catalogue and reporting, and can add workers who record day-to-day stock movements from sales and purchases.

The backend keeps the business rules close to the data: item barcodes are unique, product stock cannot be sold below zero, workers are limited per business, operational records are scoped to the authenticated user's business, and important actions are written to an audit log.

## Business Flow

The system starts with the business owner. An owner registers with a username, email address, password, and `businessName`. Registration creates the owner account, creates the business tenant, links the owner to that business, and assigns the `Owner` privilege. After login, the owner receives a Bearer JWT that includes the user's business id and business name for client routing.

Every new business starts on a 14 day `FREE_TRIAL` in `TRIAL` status. The owner can view plan cards and a billing dashboard, then create a PayPal-backed `PLUS` or `PRO` subscription. PayPal approval creates a pending local subscription; manual activation or verified PayPal webhooks move the business to `ACTIVE`, update the current billing period, and write billing audit records. Payment failure, cancellation, suspension, expiry, expired trials, and unpaid active periods move the business to restricted states so product and barcode features can be blocked until billing is corrected.

Once authenticated, the owner prepares the operating environment:

1. Roles are seeded automatically at startup: `Owner` and `Worker`.
2. The owner creates worker accounts according to the active plan: 2 on free trial, 5 on Plus, and unlimited on Pro.
3. The owner creates business-scoped products with a name, category, unit price, returnable pricing settings, nightshift pricing settings, and opening stock quantity.
4. Products start in `CREATED` status.
5. Workers assign physical item barcodes to those products, one barcode per stocked unit. Returnable product barcodes start in `NOT_CLAIMED` status and can store a `referencee` cellphone reference for later claims. Non-returnable product barcodes are stored as `NOT_CLAIMABLE` because there is no returnable amount to claim. For example, a product with stock quantity `20` can receive up to `20` barcodes.
6. Once barcode count equals stock quantity, workers submit the product for approval and the product moves to `PENDING_APPROVAL`.
7. Products can be marked as returnable, with an owner-configured packaging/deposit charge added to sale prices.
8. Owners must categorize products as `Alcohol` or `NonAlcohol`, which allows reporting to separate alcohol movement from non-alcohol stock.

Daily operations happen through inventory transactions. A transaction is created with:

- a transaction type: `BUY` or `SELL`
- the worker who handled the movement
- the owner responsible for oversight
- one or more product line items

When a `BUY` transaction is recorded, product stock increases by the purchased quantity. When a `SELL` transaction is recorded, product stock decreases by the sold quantity. The API rejects a sale if there is not enough stock available, which protects the inventory record from drifting away from reality.

For barcode-backed sale screens, each `SELL` transaction item must include a `barcodes` array. The backend requires the barcode count to match the sold quantity, requires the values to be unique, and verifies every barcode belongs to the selected product. `BUY` transactions increase stock and must not include barcode values.

Sale prices are calculated from the product base price plus active product charges:

- returnable products add the configured `returnablePrice`
- products with nightshift pricing add the configured `nightShiftPrice` while the current business time is inside the configured nightshift window
- `BUY` transactions use only the provided override price or product base price
- returnable barcodes that are still `NOT_CLAIMED` are moved to `EXPIRED` at the Friday 17:00 business-time cutoff

The reporting flow gives the owner a management view of the business:

- alcohol reports show bought and sold quantities and values for alcohol products over a date range
- inventory summary shows product counts, category totals, total stock quantity, total stock value, and low-stock counts
- product movement reports show bought and sold movement per product, sorted by sold quantity

Audit logging completes the loop. Owner registration, worker creation, product creation, barcode assignment, barcode claims, and transaction creation are recorded with an action name, entity type, entity id, actor username, business id, details, and timestamp. This gives the owner a tenant-scoped timeline of sensitive business events.

## Core Capabilities

| Area | What it does |
| --- | --- |
| Authentication | Registers owners with a business tenant, authenticates users, and issues JWT access tokens with business claims. |
| Authorization | Protects owner-only actions while allowing workers to record operational transactions. |
| User management | Lists business users, creates up to two business workers, and exposes the current authenticated profile. |
| Product catalogue | Creates business products, tracks worker-assigned item barcodes, enforces unique barcodes, and supports barcode and reference lookup inside the actor's business. |
| Stock movement | Records business-scoped `BUY` and `SELL` transactions, requires scanned item barcode rows for sales, and updates stock automatically. |
| Reporting | Produces tenant-scoped alcohol, inventory summary, and product movement reports. |
| Billing | Exposes Free Trial, Plus, and Pro plan rules, creates PayPal approval subscriptions, processes verified webhook lifecycle events, and records billing audit logs. |
| Audit trail | Stores key business events with business ownership for traceability. |
| Database migrations | Uses Flyway migrations for the initial schema, audit logs, product barcode assignments, returnable pricing, lifecycle statuses, transaction item barcodes, barcode claim references, business tenancy, email verification, and billing. |

## Authorization Model

| Role | Purpose | Main access |
| --- | --- | --- |
| `Owner` | Business owner and administrator | Create products, update product quantity, create workers, view users, view transactions, run reports, view audit logs. |
| `Worker` | Operational worker | Log in, read products, add product barcodes, submit products for approval, view own profile, and create inventory transactions. |

Public routes are limited to owner registration and login. All other API routes require a valid Bearer token.

## API Overview

### Health

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/health` | Public | Runtime liveness check for hosting platforms. |
| `GET` | `/api/health` | Public | API-prefixed liveness check for frontend startup checks. |
| `GET` | `/ready` | Public | Runtime readiness check that verifies database connectivity. |
| `GET` | `/api/ready` | Public | API-prefixed readiness check for deploy probes and frontend diagnostics. |

### Authentication

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | Register an owner account and create the business tenant. |
| `POST` | `/api/auth/login` | Public | Authenticate and receive a Bearer JWT. |
| `POST` | `/api/auth/forgot-password` | Public | Request a password reset email. |
| `POST` | `/api/auth/reset-password` | Public | Reset a password using a valid reset token. |
| `GET` | `/api/auth/verify-email?token=...` | Public | Verify an owner's email address. |
| `POST` | `/api/auth/resend-verification` | Public | Request another email verification link. |

### Users

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/users` | Owner | List users in the owner's business with pagination. |
| `POST` | `/api/users/workers` | Owner | Create a worker account in the owner's business. Maximum workers per business: 2. |
| `GET` | `/api/users/me` | Authenticated | Return the current authenticated user. |
| `GET` | `/api/users/{id}` | Owner | Return a user by id within the owner's business. |

### Privileges

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/privileges` | Authenticated | List privileges. |
| `GET` | `/api/privileges/{role}` | Authenticated | Get one privilege by role name. |
| `POST` | `/api/privileges` | Owner | Create a privilege if it does not already exist. |

### Products

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/products` | Owner | Create a product with stock quantity and no barcodes. |
| `POST` | `/api/products/{id}/barcodes` | Worker | Assign one barcode to one stocked unit of a product, optionally with `referencee`. |
| `PATCH` | `/api/products/{id}/quantity` | Owner | Update product stock quantity. Quantity cannot be lower than assigned barcode count. |
| `POST` | `/api/products/{id}/submit-approval` | Worker | Submit the product for approval once barcode count equals stock quantity. |
| `GET` | `/api/products` | Authenticated | List products in the authenticated user's business with pagination. |
| `GET` | `/api/products/{id}` | Authenticated | Get a product by id within the authenticated user's business. |
| `GET` | `/api/products/barcode/{barcode}` | Authenticated | Get a business product by an assigned barcode. |

### Product Barcodes

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/barcodes/reference/{referencee}` | Authenticated | Find barcode claim rows by customer reference in the authenticated user's business. |
| `POST` | `/api/barcodes/reference/{referencee}/claim` | Authenticated | Claim one active returnable barcode when the reference has one unambiguous `NOT_CLAIMED` match. |
| `POST` | `/api/barcodes/{id}/claim` | Authenticated | Claim a selected active returnable barcode row by id after reference search. |

### Transactions

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/transactions` | Authenticated | Create a `BUY` or `SELL` transaction. |
| `GET` | `/api/transactions` | Owner | List transactions in the owner's business with pagination. |
| `GET` | `/api/transactions/{id}` | Owner | Get a transaction by id within the owner's business. |

### Reports

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/reports/alcohol` | Owner | Business alcohol bought and sold totals over a date range. |
| `GET` | `/api/reports/inventory-summary` | Owner | Business product and stock summary, including low-stock count. |
| `GET` | `/api/reports/product-movement` | Owner | Business bought and sold movement per product over a date range. |

### Audit Logs

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/audit-logs` | Owner | List audit logs in the owner's business with pagination. |

### Billing

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/billing/plans` | Authenticated | List Free Trial, Plus, and Pro plan cards and feature flags. |
| `GET` | `/api/billing/me` | Owner | Return the owner's current business billing state and latest subscription. |
| `GET` | `/api/billing/dashboard` | Owner | Return the billing dashboard, trial days left, available plans, and feature access flags. |
| `POST` | `/api/billing/subscriptions` | Owner | Create a paid PayPal subscription approval flow for Plus or Pro. |
| `POST` | `/api/billing/subscriptions/{subscriptionId}/activate` | Owner | Activate an approved PayPal subscription after PayPal reports an approved/active status. |

### Admin Billing

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/admin/businesses/{businessId}/billing/override` | Owner | Manually reactivate, deactivate, cancel, or mark a business past due. |
| `GET` | `/api/admin/businesses/{businessId}/billing/audit-logs` | Owner | List billing audit entries for a business. |

### PayPal Webhooks

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/paypal/webhooks` | Public PayPal callback | Verify PayPal headers, ignore duplicate events, and process subscription/payment lifecycle events. |

## Example Requests

Register the owner:

```json
{
  "username": "owner",
  "emailAddress": "owner@example.com",
  "password": "secret",
  "businessName": "Owner Retail Store"
}
```

Log in:

```json
{
  "username": "owner",
  "password": "secret"
}
```

Create a product:

```json
{
  "name": "Non alcohol",
  "category": "NonAlcohol",
  "price": 20.50,
  "returnableEnabled": true,
  "returnablePrice": 3.00,
  "nightShiftEnabled": false,
  "nightShiftPrice": null,
  "nightShiftStartTime": null,
  "nightShiftEndTime": null,
  "stockQuantity": 20
}
```

Assign a barcode to one stocked unit:

```json
{
  "barcode": "6001"
}
```

Update product quantity:

```json
{
  "stockQuantity": 20
}
```

Create a sale transaction:

```json
{
  "type": "SELL",
  "employeeId": 2,
  "ownerId": 1,
  "items": [
    {
      "productId": 9,
      "quantity": 2,
      "unitPrice": 20.50,
      "barcodes": ["6001", "6002"]
    }
  ]
}
```

If `unitPrice` is omitted on a transaction item, the current product price is used. For `SELL` items, `barcodes` is required and its length must match `quantity`. `BUY` items must omit `barcodes`.

Create a paid subscription approval flow:

```json
{
  "plan": "PLUS",
  "billingInterval": "MONTHLY",
  "termYears": null
}
```

## Data Model

The core database tables are:

- `privileges`: role names used for authorization.
- `businesses`: tenant record created during owner registration and linked to the owner.
- `tracker_users`: owner and worker accounts, linked to privileges and businesses.
- `products`: business product catalogue with category, price, stock quantity, returnable flag, and lifecycle status such as `CREATED` or `PENDING_APPROVAL`.
- `product_barcodes`: worker-assigned physical item barcodes linked to products, with `referencee` and barcode status such as `NOT_CLAIMED`, `CLAIMED`, `EXPIRED`, or `NOT_CLAIMABLE`.
- `inventory_transactions`: business transaction header with date, type, employee, and owner.
- `transaction_items`: product line items for each transaction.
- `transaction_item_barcodes`: barcode values attached to transaction item rows for frontend transaction tables.
- `business_subscriptions`: local subscription records, approval URLs, PayPal ids, plan, interval, term, amount, status, and billing period.
- `paypal_webhook_events`: raw PayPal webhook payloads and processing statuses for idempotency and traceability.
- `billing_audit_logs`: billing lifecycle, webhook, payment, and admin override history.
- `email_verification_tokens` and `password_reset_tokens`: hashed one-time account recovery and verification tokens.
- `audit_logs`: key business events, tenant ownership, and actor traceability.

Flyway migrations live in `src/main/resources/db/migration`.

## Tech Stack

- Java 25
- Spring Boot 4
- Spring Web MVC
- Spring Security
- Spring OAuth2 Resource Server with JWT
- Spring Data JPA
- Flyway
- H2 for local/runtime support
- PostgreSQL for staging and production
- Maven Wrapper

## Configuration

Base application settings live in `src/main/resources/application.properties`.

Important environment variables:

| Variable | Purpose |
| --- | --- |
| `JWT_SECRET` | Secret used to sign and verify JWTs. Required for staging and production. |
| `JWT_EXPIRATION_MINUTES` | Token lifetime. Defaults to 120 minutes locally and staging, 60 in production. |
| `SUPABASE_DB_URL` | PostgreSQL database URL for staging and production. |
| `SUPABASE_DB_USER` | PostgreSQL username. Defaults to `postgres`. |
| `SUPABASE_DB_PASSWORD` | PostgreSQL password. |
| `DB_MAX_POOL_SIZE` | Maximum database pool size. |
| `DB_MIN_IDLE` | Minimum idle database connections. |
| `DB_CONNECTION_TIMEOUT_MS` | Database connection timeout in milliseconds. |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins allowed to call the API, such as `https://www.kingsparkon.com,http://localhost:3000`. |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | Optional comma-separated origin patterns, such as `https://*.vercel.app`, for preview deployments. |
| `CORS_ALLOWED_METHODS` | Optional comma-separated allowed HTTP methods. Defaults to `GET,POST,PUT,PATCH,DELETE,OPTIONS`. |
| `CORS_ALLOWED_HEADERS` | Optional comma-separated allowed request headers. Defaults to auth and JSON headers used by the Next.js UI. |
| `RATE_LIMIT_ENABLED` | Enables the in-memory API rate limiter. Defaults to `true`. |
| `RATE_LIMIT_PUBLIC_AUTH_LIMIT`, `RATE_LIMIT_PUBLIC_AUTH_WINDOW_SECONDS`, `RATE_LIMIT_PUBLIC_AUTH_RETRY_AFTER_SECONDS` | IP-based limits for public auth endpoints such as login, register, forgot password, reset password, resend verification, and email verification. Defaults to 10 requests per 60 seconds with a 60 second retry. |
| `RATE_LIMIT_FREE_TRIAL_LIMIT`, `RATE_LIMIT_FREE_TRIAL_WINDOW_SECONDS`, `RATE_LIMIT_FREE_TRIAL_RETRY_AFTER_SECONDS` | Business-plan API limit for free trial tenants. Defaults to 30 requests per 60 seconds with a 30 second retry wait. |
| `RATE_LIMIT_PLUS_LIMIT`, `RATE_LIMIT_PLUS_WINDOW_SECONDS`, `RATE_LIMIT_PLUS_RETRY_AFTER_SECONDS` | Business-plan API limit for Plus tenants. Defaults to 120 requests per 60 seconds with a 15 second retry wait. |
| `RATE_LIMIT_PRO_LIMIT`, `RATE_LIMIT_PRO_WINDOW_SECONDS`, `RATE_LIMIT_PRO_RETRY_AFTER_SECONDS` | Business-plan API limit for Pro tenants. Defaults to 600 requests per 60 seconds with a 5 second retry wait. |
| `SPRINGDOC_API_DOCS_ENABLED`, `SPRINGDOC_SWAGGER_UI_ENABLED` | Optional Swagger/OpenAPI toggles. The production profile disables both by default. |
| `FRONTEND_RESET_PASSWORD_URL` | Frontend reset password page used in reset email links. |
| `APP_MAIL_ENABLED` | Enables outbound email sending when `true`. |
| `APP_MAIL_FROM` | Sender address used for application emails. |
| `FRONTEND_EMAIL_VERIFICATION_URL` | Frontend email verification page used in verification email links. |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP connection settings. |
| `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | SMTP authentication and TLS toggles. |
| `PAYPAL_BASE_URL` | PayPal REST API base URL. Defaults to PayPal sandbox locally. |
| `PAYPAL_CLIENT_ID` | PayPal REST API client id. |
| `PAYPAL_CLIENT_SECRET` | PayPal REST API client secret. |
| `PAYPAL_WEBHOOK_ID` | PayPal webhook id used during signature verification. |
| `PAYPAL_RETURN_URL` | Frontend URL PayPal sends the owner to after approval. |
| `PAYPAL_CANCEL_URL` | Frontend URL PayPal sends the owner to after cancellation. |
| `PAYPAL_BILLING_PLUS_MONTHLY_PLAN_ID`, `PAYPAL_BILLING_PLUS_YEARLY_PLAN_ID` | PayPal plan ids for Plus billing intervals. |
| `PAYPAL_BILLING_PRO_MONTHLY_PLAN_ID`, `PAYPAL_BILLING_PRO_YEARLY_PLAN_ID` | PayPal plan ids for Pro billing intervals. |

The default JWT secret in local configuration is for development only. Replace it before staging or production.

Rate-limited responses return HTTP `429` with `Retry-After`, `X-RateLimit-Policy`, `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset` headers. The JSON body also includes `retryAfterSeconds`, so the Next.js UI can show a plan-specific wait state. The defaults intentionally make free trial tenants wait 30 seconds and Plus tenants wait 15 seconds when their plan limit is reached.

## Running Locally

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS or Linux:

```bash
./mvnw spring-boot:run
```

The API starts with the application name `backend`. Protected endpoints require:

```http
Authorization: Bearer <access-token>
```

## Running Tests

On Windows:

```powershell
.\mvnw.cmd test
```

To generate a measured JaCoCo coverage report:

```powershell
.\mvnw.cmd verify
```

The HTML report is written to `target/site/jacoco/index.html`, and the XML report is written to `target/site/jacoco/jacoco.xml`.

On macOS or Linux:

```bash
./mvnw test
```

The `pom.xml` targets Java 25. On this Windows checkout, use a JDK 25+ `JAVA_HOME`; for example:

```powershell
$env:JAVA_HOME='C:\Users\thand\.jdks\openjdk-26.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

The current focused billing/access/webhook/claim test slice can be run with:

```powershell
.\mvnw.cmd "-Dtest=BusinessPlanPolicyServiceTest,BusinessAccessServiceTest,BusinessBillingServiceTest,PayPalWebhookServiceTest,ProductBarcodeClaimServiceTest,AdminBusinessOverrideServiceTest" test
```

## Error Handling

The API returns structured error responses with:

- `timestamp`
- `status`
- `error`
- `message`

Validation and business rule failures return clear client-facing messages, such as duplicate usernames, duplicate barcodes, missing records, invalid credentials, invalid date ranges, and insufficient stock.

## Repository Structure

```text
src/main/java/com/king_sparkon_tracker/backend
  controller/   REST API controllers
  dto/          Request and response objects
  exception/    API error handling and domain exceptions
  model/        JPA entities and enums
  repository/   Spring Data repositories
  security/     JWT, security filter chain, and role seeding
  service/      Business logic and transaction rules

src/main/resources
  application.properties
  application-staging.properties
  application-prod.properties
  db/migration/
```

## Business Rules Worth Protecting

- Owner registration creates one business tenant and links the owner to that tenant.
- Free trial businesses can have 2 workers, Plus businesses can have 5 workers, and Pro businesses can have unlimited workers.
- Businesses must be in `TRIAL` or `ACTIVE` status to use protected operational features.
- Pro-only features such as worker tips, Business Analysis AI, and worker clocker are blocked on Free Trial and Plus.
- Owners cannot buy the `FREE_TRIAL` plan; paid subscriptions must be Plus or Pro.
- PayPal webhook signatures must verify before a billing lifecycle event is processed.
- Duplicate PayPal webhook events are skipped by event id.
- Payment failure marks a business `PAST_DUE`; cancellation, suspension, expiry, and expired billing periods deactivate access.
- Admin billing overrides must record audit history.
- Products, users, transactions, reports, barcode lookups, barcode claims, and audit logs are scoped to the authenticated user's business.
- Product barcodes must be unique.
- Returnable product barcodes start as `NOT_CLAIMED`; non-returnable product barcodes start as `NOT_CLAIMABLE`.
- Returnable product barcodes that are still `NOT_CLAIMED` expire at the Friday 17:00 business-time cutoff.
- Returnable product barcodes can be searched and claimed by `referencee`.
- Product status starts as `CREATED`.
- Products can only be submitted for approval when barcode count equals stock quantity.
- Product quantity cannot be updated below the assigned barcode count.
- Product price and opening stock cannot be negative.
- Returnable products add the configured `returnablePrice` to sale price.
- Products with nightshift pricing add the configured `nightShiftPrice` during the configured nightshift window.
- `BUY` transactions cannot include barcodes.
- Transaction quantities must be positive.
- `SELL` transactions require scanned barcodes.
- `SELL` transaction barcodes must belong to the selected product.
- `SELL` transaction barcode count must match sold quantity.
- `SELL` transactions cannot reduce stock below zero.
- Reports reject invalid date ranges where `from` is after `to`.
- Sensitive actions are recorded in audit logs.
