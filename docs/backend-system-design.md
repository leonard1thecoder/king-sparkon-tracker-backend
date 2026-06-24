# King Sparkon Tracker Backend System Design

## Production-readiness target

This backend is a Spring Boot API for barcode inventory, business onboarding, user management, payments, tips, subscribers, affiliates, promotions, reports, audit logs, and withdrawals. The frontend should treat this document as the integration contract for dashboard, onboarding, checkout, promotion, and admin screens.

## Architecture

```text
Frontend dashboard / public pages
        |
        | HTTPS JSON API + Bearer JWT
        v
Spring Boot backend
        |
        | JPA/Flyway
        v
PostgreSQL
        |
        | Spring Cache
        v
Redis
        |
        | Webhooks / REST APIs
        v
Stripe, PayPal, SMTP, Twilio WhatsApp
```

## Runtime components

- Spring Boot application with stateless JWT security.
- PostgreSQL as the system-of-record database.
- Flyway migrations with `spring.jpa.hibernate.ddl-auto=validate`.
- Redis-backed Spring Cache for stable reference, feature policy, billing-plan, and pricing data.
- Stripe for website-payment links and payment webhooks.
- PayPal for billing and payout onboarding flows.
- SMTP + Thymeleaf templates for email notifications.
- Twilio WhatsApp REST integration for WhatsApp promotions.
- Docker build runs `mvn -B clean verify` before producing the runtime image.

## Security model

### Public endpoints

Frontend may call these without a JWT:

- `POST /api/auth/register`
- `POST /api/auth/register-admin`
- `POST /api/auth/register-affiliate`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`
- `POST /api/auth/resend-verification`
- `GET /api/auth/verify-email`
- `POST /api/contact-inquiries`
- `POST /api/paypal/webhooks`
- `POST /api/stripe/webhooks`
- `POST /api/subscribers`
- `DELETE /api/subscribers?contact=...`
- Public affiliate link click/random endpoints.

### Authenticated endpoints

- `GET /api/users/me`
- `PATCH /api/users/me/onboarding`
- Product/barcode read endpoints.
- Transaction creation endpoints.

### Owner endpoints

- Worker management.
- Product creation and stock management.
- Billing management.
- Promotions under `/api/promotions/**`.
- Reports, audit logs, transaction history, tip management, withdrawals.

### Admin endpoints

- Platform admin APIs under `/api/admin/**`.
- Admin registered-subscriber promotions.

### Affiliate endpoints

- Affiliate dashboard APIs under `/api/affiliates/**`.

## User roles

| Role | Purpose | Frontend area |
| --- | --- | --- |
| `Owner` | Owns a business tenant, manages products, workers, transactions, promotions, tips, billing, reports. | Owner dashboard |
| `Worker` | Scans barcodes, creates sales, optionally receives tips through QR/payment links. | Worker dashboard/mobile scanner |
| `Affiliate` | Promotes King Sparkon Tracker and referred businesses. | Affiliate dashboard |
| `Admin` | Oversees all businesses, users, and platform promotions. | Admin dashboard |

## Business feature policy

Feature access is determined from business status + business plan. The backend caches feature-policy decisions by business id, plan, status, and feature so a plan/status change naturally uses a new Redis key.

| Feature | Free trial | Plus | Pro |
| --- | --- | --- | --- |
| `CREATE_WORKERS` | Yes, max 2 workers | Yes, max 5 workers | Yes, unlimited workers |
| `CREATE_PRODUCTS` | Yes | Yes | Yes |
| `ADD_BARCODES` | Yes | Yes | Yes |
| `SCAN_BARCODES` | Yes | Yes | Yes |
| `WORKER_TIPS_PLATFORM` | No | No | Yes |
| `BUSINESS_ANALYSIS_AI` | No | No | Yes |
| `WORKER_CLOCKER` | No | No | Yes |

Frontend should still render locked Pro-only features by plan, but backend remains the source of truth.

## Owner onboarding business logic

### Endpoint

`POST /api/auth/register`

### Frontend payload

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

### Rules

- `businessName` is required.
- `businessDescription` is optional, max 2000 characters, trimmed before persistence.
- If both `physicalAddress` and `cellphoneNumber` are present, owner onboarding is completed immediately.
- If `affiliateCode` is present and valid, the business is linked to that affiliate.
- Email verification is sent after registration.

### Frontend behavior

- Ask for business description on the same screen as business name.
- Show onboarding as completed only if address and cellphone are captured.
- Show email-verification pending state after successful registration.

## Website payment and subscriber capture

### Endpoint

`POST /api/transactions`

### Frontend payload for website payment

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

### Rules

- `paymentEmail` remains the backward-compatible email used for Stripe website payment delivery and returnable barcode references.
- `paymentContact` can be an email address or an international cellphone number.
- When `paymentType` is `WEBSITE_PAYMENT`, `paymentContact` is stored on the transaction and automatically subscribed as a `CLIENT` subscriber.
- For non-website payments such as `CASH`, payment contact is not auto-subscribed.
- Cellphone numbers must use international format, for example `+27821234567`.

### Frontend behavior

- For website payments, ask for either email or cellphone as customer contact.
- If cellphone is used for subscriber capture, still provide a valid `paymentEmail` when Stripe email delivery is required.
- Display `paymentContact` in transaction detail and payment-link audit screens.

## Subscriber model

### Endpoint

`POST /api/subscribers`

### Payload examples

King Sparkon subscriber:

```json
{
  "contact": "client@example.com",
  "subscriberType": "KINGSPARKON_SUBSCRIBER",
  "preferredChannel": "EMAIL"
}
```

Affiliate lead not yet registered:

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

### Rules

- Contact can be email or cellphone.
- Emails are normalized lowercase.
- Cellphones are compacted and must start with `+`.
- Direct signup defaults to `KINGSPARKON_SUBSCRIBER`.
- Tip payment-link clients and website-payment clients become `CLIENT` subscribers.
- Existing inactive subscribers are reactivated and updated.

## Promotion business logic

### Owner promotions

`POST /api/promotions`

Owners can create promotions for subscribers. Price is calculated by active audience size.

### Admin registered-subscriber promotions

`POST /api/admin/promotions/registered-subscribers`

Admins can create promotions targeting registered subscriber/affiliate users.

### Automated system promotions

Scheduler runs hourly and creates platform promotions:

- King Sparkon Tracker service promotion.
- Affiliate program promotion for unregistered affiliate leads.

### Audience enum

- `ALL_SUBSCRIBERS`
- `REGISTERED_AFFILIATES`
- `UNREGISTERED_AFFILIATES`
- `REGISTERED_SUBSCRIBERS`

### Channel enum

- `EMAIL`
- `WHATSAPP`
- `ANY`

### Anti-crowding rule

A subscriber must not receive more than one promotion notification inside a 2-day window. The backend uses `Subscriber.lastNotifiedAt` plus notification logs to enforce this.

### Pricing

| Audience size | Platform fee | Subscriber rate |
| --- | ---: | ---: |
| 1-100 | R49 | R0.90 |
| 101-1000 | R49 | R0.65 |
| 1001+ | R49 | R0.45 |

Frontend should call `GET /api/promotions/quote` before confirming paid/bulk promotion purchase.

## Tips and withdrawals

### Tip payment links

`POST /api/tips`

Tip payment-link creation supports `clientContact`; when present, the client is subscribed by default.

```json
{
  "workerId": 12,
  "tipAmount": 50.00,
  "callbackUrl": "https://app.example/tips/complete",
  "clientContact": "client@example.com"
}
```

### Withdrawal rule

Tip withdrawal net amount uses the configured `app.tips.withdrawal-fee-percent`. The removed 2.03% additional fee is not part of the current production logic.

## Redis caching design

### Enabled profile

Use profile `redis` locally or in production:

```bash
SPRING_PROFILES_ACTIVE=redis
```

### Redis properties

```properties
spring.cache.type=redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.timeout=${REDIS_TIMEOUT:2s}
```

### Cache names and TTLs

| Cache | TTL | Purpose |
| --- | ---: | --- |
| `privileges` | 12 hours | Stable role list for UI/settings/admin screens |
| `privilegeByRole` | 12 hours | Role lookup during user creation/admin flows |
| `billingPlans` | 6 hours | Public billing plan cards and frontend plan comparison |
| `affiliateCommissionTiers` | 6 hours | Affiliate commission display rules |
| `businessPlanPrices` | 6 hours | Plan monthly price lookups |
| `businessPlanWorkerLimits` | 6 hours | Worker-limit lookups per plan |
| `businessFeatureAccess` | 15 minutes | Feature access decisions keyed by business id, plan, status, and feature |
| `promotionQuotes` | 10 minutes | Bulk promotion pricing quotes |

### Cache invalidation and safety

- Creating a privilege evicts role caches.
- Business feature access uses business id + plan + status + feature as the cache key, so plan/status changes produce new cache entries instead of reusing old decisions.
- Business feature access has a shorter TTL than billing plans because subscription status can change.
- Promotion pricing cache is short-lived because audience count changes frequently.
- Do not cache whole business or user entities for authorization decisions.

## Local Redis with Docker Compose

Run Redis locally:

```bash
 docker compose -f docker-compose.redis.yml up -d
```

Run backend with Redis profile:

```bash
SPRING_PROFILES_ACTIVE=redis REDIS_HOST=localhost mvn spring-boot:run
```

Build full Docker image with tests:

```bash
./scripts/full-maven-scan.sh
docker build -t king-sparkon-tracker-backend:local .
```

## Frontend development checklist

### Owner onboarding screen

- Fields: username, email, password, businessName, businessDescription, localizationCountry, physicalAddress, cellphoneNumber, affiliateCode.
- After success: show verify-email screen.

### Subscriber forms

- Public newsletter/signup form should call `POST /api/subscribers`.
- Affiliate landing page should pass `subscriberType=AFFILIATE` and `affiliateRegistered=false` until the user completes affiliate registration.

### Admin dashboard

- Admin promotion form should call `POST /api/admin/promotions/registered-subscribers`.
- Admin should see users/businesses through `/api/admin/users` and `/api/admin/businesses`.

### Owner promotion dashboard

- Quote first: `GET /api/promotions/quote`.
- Create campaign: `POST /api/promotions`.
- List campaigns: `GET /api/promotions`.

### Transaction/payment screen

- For website payments, collect `paymentEmail` plus optional `paymentContact`.
- Show returned `paymentUrl`, `paymentStatus`, `paymentContact`, and `paymentReference`.

### Worker/tips screen

- Worker creates tip link/QR through `/api/tips`.
- Tip form should pass `clientContact` when customer email/cellphone is available.

## Production deployment notes

Before Cloud Run production deployment:

1. Run `./scripts/full-maven-scan.sh`.
2. Build Docker image.
3. Run Flyway against staging database.
4. Confirm Redis connectivity.
5. Configure JWT secret, PostgreSQL, Stripe, PayPal, SMTP, Twilio, CORS, and frontend URLs.
6. Validate webhook signatures for Stripe and PayPal.
7. Smoke-test owner registration, login, product creation, website transaction, tip payment, subscriber signup, promotion send, and unsubscribe.

Cloud Run credentials should not be committed to the repository. Use Google Secret Manager and Cloud Run environment variables.
