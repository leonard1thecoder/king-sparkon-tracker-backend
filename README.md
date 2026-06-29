# King Sparkon Tracker Backend

Spring Boot backend for King Sparkon Tracker: barcode inventory, business tenants, workers, affiliates, tickets, tips, Stripe payments, business account top-ups, promotions, payouts, reports, audit logs, and user dashboards.

This backend is designed for Google Cloud Run with PostgreSQL, Flyway, JWT sessions, Redis-compatible caching/rate limiting, Stripe payment links, PayPal billing/payout support, email, WhatsApp notifications, Actuator metrics, Prometheus, Grafana, and Docker CI.

> Payment note: this system uses Stripe-backed payment links for tips, ticket checkout, business account top-ups, and website transactions. Google Pay placeholders were removed because Google Pay is not part of the current implementation scope.

## Key Features Added

| Area | Capability |
| --- | --- |
| User dashboard | Users can view businesses, business workers for tips, published business events, and a combined dashboard feed. |
| Worker tips | Users can create worker tip payments and receive a Stripe payment URL plus QR code. |
| Ticket events | Users can view ticket events by business and purchase event tickets through Stripe checkout links. |
| Ticket QR | Purchased tickets generate ticket references and QR payloads for worker verification. |
| Business account | Owners can top up an in-app business account and use that balance for owner promotions and ticket event boosts. |
| Posters and profiles | Ticket events support poster photo URLs; workers, users, and affiliates support profile-picture fields. Google Cloud image hosting is planned separately. |
| Affiliates | Affiliate onboarding supports payout/profile data and affiliate referral QR/link fields. |
| CI hardening | Docker CI runs Maven verification through the backend build gate. |
| Tests | Focused unit tests cover ticket business rules, onboarding profile data, business account debit rules, and user dashboard/tip flows. |

## System Architecture

```mermaid
flowchart LR
    UI[Next.js Web App] --> API[Spring Boot API on Cloud Run]
    API --> DB[(PostgreSQL)]
    API --> REDIS[(Redis or Memorystore)]
    API --> STRIPE[Stripe Payment Links]
    API --> PAYPAL[PayPal Billing/Payouts]
    API --> SMTP[Email Provider]
    API --> TWILIO[Twilio WhatsApp]
    API --> PROM[Actuator / Prometheus]
    PROM --> GRAFANA[Grafana]
    STRIPE --> API
    PAYPAL --> API
```

## User Dashboard and Payments Flow

```mermaid
sequenceDiagram
    actor User
    actor Owner
    actor Worker
    participant UI as Next.js UI
    participant API as Backend API
    participant Stripe
    participant DB as PostgreSQL

    User->>UI: Open user dashboard
    UI->>API: GET /api/user-dashboard
    API->>DB: Load businesses + published events
    API-->>UI: businesses + events

    User->>UI: View workers for a business
    UI->>API: GET /api/user-dashboard/businesses/{businessId}/workers
    API->>DB: Load workers
    API-->>UI: worker tip cards

    User->>UI: Tip worker
    UI->>API: POST /api/user-dashboard/tips
    API->>DB: Create UNPAID tip
    API->>Stripe: Create worker tip payment link
    API-->>UI: paymentUrl + qrCodeUrl
    User->>Stripe: Pay tip

    Owner->>UI: Create ticket event
    UI->>API: POST /api/v1/tickets/events
    API->>DB: Save event + ticket types

    User->>UI: Purchase event ticket
    UI->>API: POST /api/v1/tickets/me/purchase
    API->>DB: Reserve/sell ticket inventory
    API->>Stripe: Create ticket checkout payment link
    API-->>UI: paymentUrl + qrCodeUrl + ticket references
    User->>Stripe: Pay ticket checkout

    Worker->>UI: Scan ticket QR
    UI->>API: POST /api/v1/tickets/verify
    API->>DB: Mark valid ticket as USED
```

## Component UML

```mermaid
classDiagram
    class UserDashboardController {
      +dashboard() UserDashboardResponse
      +businesses() List~BusinessCardResponse~
      +workers(businessId) List~WorkerTipCardResponse~
      +events(businessId) List~TicketEventResponse~
      +tipWorker(TipRequest) TipResponse
    }

    class UserDashboardService {
      +dashboard() UserDashboardResponse
      +businesses() List~BusinessCardResponse~
      +workersForBusiness(Long) List~WorkerTipCardResponse~
      +eventsForBusiness(Long) List~TicketEventResponse~
      +tipWorker(TipRequest) TipResponse
    }

    class TipService {
      +createTip(TipRequest) TipResponse
      +updateTipStatus(Long, UpdateTipStatusRequest) TipResponse
      +getTipsForWorker(Long) List~TipResponse~
    }

    class TicketSecureController {
      +purchase(PurchaseTicketsRequest) TicketPurchaseResponse
      +tickets() List~UserTicketResponse~
      +boostEvent(eventId, PromoteTicketEventRequest) TicketEventPromotionResponse
    }

    class TicketManagementService {
      +getUpcomingEvents() List~TicketEventResponse~
      +purchaseTickets(PurchaseTicketsRequest) TicketPurchaseResponse
      +getMyTickets(userId) List~UserTicketResponse~
      +verifyByQr(value, workerId) TicketVerificationResponse
      +promoteEvent(eventId, request, actorUsername) TicketEventPromotionResponse
    }

    class StripeService {
      +createTipPaymentLink(Tip, fee, net, callbackUrl) CreatedTipPaymentLink
      +createTicketPaymentLink(TicketPayment, TicketEvent, callbackUrl) CreatedTicketPaymentLink
      +createBusinessTopUpPaymentLink(Business, amount, callbackUrl, paymentMethod) CreatedBusinessTopUpPaymentLink
      +createTransactionPaymentLink(InventoryTransaction) CreatedTransactionPaymentLink
    }

    class BusinessAccountService {
      +createTopUp(amount, method, actor) BusinessAccountLedgerEntry
      +confirmTopUp(entryId, actor) BusinessAccountLedgerEntry
      +debitPromotion(business, amount, type, note, actor) BusinessAccountLedgerEntry
    }

    UserDashboardController --> UserDashboardService
    UserDashboardService --> TipService
    UserDashboardService --> TicketManagementService
    UserDashboardService --> BusinessRepository
    UserDashboardService --> TrackerUserRepository
    TipService --> StripeService
    TicketSecureController --> TicketManagementService
    TicketManagementService --> StripeService
    TicketManagementService --> BusinessAccountService
```

## Full ERD

```mermaid
erDiagram
    privileges ||--o{ tracker_users : grants
    businesses ||--o{ tracker_users : contains
    businesses ||--o{ products : owns
    businesses ||--o{ inventory_transactions : scopes
    businesses ||--o{ business_account_ledger_entries : funds
    businesses ||--o{ promotions : owns
    tracker_users ||--o{ refresh_tokens : has
    tracker_users ||--o{ tips : receives
    tracker_users ||--o{ worker_payout_accounts : payout_account
    tracker_users ||--o{ ticket_payments : buys
    ticket_events ||--o{ event_ticket_types : offers
    ticket_events ||--o{ user_tickets : issues
    ticket_events ||--o{ ticket_payments : paid_by
    ticket_events ||--o{ ticket_event_workers : assigned_workers
    ticket_events ||--o{ ticket_event_affiliates : assigned_affiliates
    ticket_events ||--o{ ticket_event_promotions : boosted_by
    business_account_ledger_entries ||--o{ ticket_event_promotions : funds
    ticket_payments ||--o{ user_tickets : creates
    subscribers ||--o{ promotion_notifications : receives
    promotions ||--o{ promotion_notifications : sends
    tips ||--o{ tip_withdrawals : grouped_into

    tracker_users {
        bigint id PK
        string username UK
        string email_address UK
        bigint privilege_id FK
        bigint business_id FK
        string physical_address
        string cellphone_number
        boolean onboarding_completed
        string profile_picture_url
        string job_title
        boolean tip_qr_code_enabled
        string tip_qr_code_url
        string affiliate_code UK
        string affiliate_promotion_url
        string affiliate_qr_code_url
        string affiliate_paypal_link
    }

    businesses {
        bigint id PK
        string name
        string description
        string business_plan
        string business_status
        string business_qr_code_url
        bigint affiliate_id FK
        string affiliate_code
    }

    tips {
        bigint id PK
        bigint worker_id FK
        decimal tip_amount
        string payment_reference
        string status
        bigint withdrawal_id FK
        timestamp created
        timestamp updated
    }

    ticket_events {
        string id PK
        string owner_id
        string name
        string description
        string location
        date event_date
        time event_time
        string banner_url
        string poster_photo_url
        string status
        timestamp created_at
        timestamp updated_at
    }

    event_ticket_types {
        string id PK
        string event_id FK
        string type
        decimal price
        int capacity
        int sold
        int available
    }

    ticket_payments {
        string id PK
        string event_id FK
        string user_id FK
        string buyer_name
        string buyer_email
        string ticket_type
        int quantity
        decimal subtotal_amount
        decimal checkout_service_fee_amount
        decimal total_amount
        string status
        string payment_provider
        string payment_reference
        timestamp created_at
    }

    user_tickets {
        string id PK
        string event_id FK
        string user_id FK
        string buyer_name
        string buyer_email
        string ticket_type
        decimal price_paid
        string qr_code_value
        string ticket_reference
        string status
        timestamp purchased_at
        timestamp used_at
        string scanned_by_worker_id
    }

    business_account_ledger_entries {
        bigint id PK
        bigint business_id FK
        string entry_type
        string status
        decimal amount
        decimal balance_after
        string payment_reference
        string payment_url
        string qr_code_url
        timestamp created_at
        timestamp posted_at
    }

    ticket_event_promotions {
        string id PK
        string event_id FK
        string owner_id
        decimal amount
        string currency
        string status
        bigint business_account_entry_id FK
        timestamp starts_at
        timestamp ends_at
    }
```

## API Contract for UI

### User dashboard

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/user-dashboard` | View businesses and upcoming events. |
| `GET` | `/api/user-dashboard/businesses` | View business cards. |
| `GET` | `/api/user-dashboard/businesses/{businessId}/workers` | View workers available for tips. |
| `GET` | `/api/user-dashboard/businesses/{businessId}/events` | View published events for that business. |
| `POST` | `/api/user-dashboard/tips` | Create a worker tip Stripe payment link. |

### Tickets

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/tickets/events` | Public/upcoming ticket events. |
| `POST` | `/api/v1/tickets/me/purchase` | Create ticket checkout and return Stripe payment URL/QR. |
| `GET` | `/api/v1/tickets/me/tickets` | View my purchased tickets. |
| `POST` | `/api/v1/tickets/me/events/{eventId}/boosts` | Owner boosts a ticket event using business-account balance. |
| `GET` | `/api/v1/tickets/me/event-boosts` | Owner views event boosts. |

### Business account

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/business-account/summary` | View owner account balance. |
| `GET` | `/api/business-account/ledger` | View ledger movements. |
| `POST` | `/api/business-account/top-ups` | Create Stripe top-up link. |
| `POST` | `/api/business-account/top-ups/{entryId}/confirm` | Confirm completed top-up. |

## Configuration

Required production secrets must be supplied by environment variables or a secret manager. Do not commit real values.

```properties
STRIPE_SECRET_KEY=
STRIPE_WEBHOOK_SECRET=
BUSINESS_ACCOUNT_TOP_UP_SUCCESS_URL=https://your-ui/dashboard/owner/account?topup=success
BUSINESS_ACCOUNT_TOP_UP_CANCEL_URL=https://your-ui/dashboard/owner/account?topup=cancelled
TICKETS_CHECKOUT_SUCCESS_URL=https://your-ui/tickets/my-tickets?stripe=success
TICKETS_CHECKOUT_CANCEL_URL=https://your-ui/tickets?stripe=cancelled
TICKETS_WITHDRAWAL_FEE_PERCENT=5.00
TICKETS_WITHDRAWAL_MINIMUM_ZAR=100.00
TICKETS_PROMOTION_PRICE_ZAR=1500.00
```

## Testing and CI

Run locally:

```bash
mvn test
```

Docker CI path:

```bash
bash scripts/full-maven-scan.sh
```

CI builds the backend Docker image and runs Maven verification. Current focused tests include:

- `TicketBusinessRulesTest`
- `BusinessAccountServiceTest`
- `OnboardingProfileServiceTest`
- `UserDashboardServiceTest`

## Deployment Notes

- Use Google Cloud Run for the Spring Boot container.
- Use Cloud SQL PostgreSQL or managed PostgreSQL.
- Use Secret Manager for Stripe, PayPal, SMTP, JWT, and DB credentials.
- Use Redis or Memorystore for distributed cache/rate limiting when running multiple instances.
- Use Actuator + Prometheus + Grafana for observability.
