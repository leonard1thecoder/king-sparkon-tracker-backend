# Phase 1 production-readiness changes

## Scope

This phase is intentionally backward-compatible and limited to API authorization foundations and payment-provider idempotency. It does not restructure the modular monolith or modify stock and ticket allocation algorithms.

## Findings

| Severity | Domain | Finding | Phase 1 fix |
| --- | --- | --- | --- |
| Critical | Worker tips | Owner-accessible tip reads and status updates accepted global worker/tip IDs without verifying business ownership. | Resolve the authenticated actor server-side and reject cross-business access before repository reads or mutations. |
| High | Stripe webhooks | The event ID was unique, but the find-then-insert flow could race under concurrent deliveries. | Claim the event in an isolated transaction and recover the persisted winner after a unique-key conflict. |
| High | PayPal webhooks | The event ID was unique, but the exists-then-insert flow could race; a signature-failed event also blocked a later valid retry. | Use the same atomic claim flow and permit controlled retry from `FAILED` or `SIGNATURE_FAILED`. |
| High | Tip creation authorization | The user dashboard exposes worker tipping, while security allowed only the Worker role to create a tip. | Permit any authenticated user to create a tip while keeping recipient eligibility checks in the application service. |
| Medium | Tenant context | Tenant checks were implemented independently in domain services. | Add a reusable immutable `AuthenticatedActor` and `AuthenticatedActorService` foundation. |

## Endpoint behavior

Existing URLs are preserved.

- `POST /api/tips` remains the tip-creation endpoint and now accepts any authenticated role.
- `GET /api/tips?status=...` is global only for administrators; owners receive only their business records.
- `GET /api/tips/worker/{workerId}` rejects workers outside the authenticated owner’s business.
- `PATCH /api/tips/{tipId}/status` rejects tips outside the authenticated owner’s business.
- `POST /api/stripe/webhooks` and `POST /api/paypal/webhooks` retain their existing contracts while concurrent duplicate deliveries are claimed safely.

## Database migrations

No Flyway migration is required in this phase. The existing Stripe and PayPal webhook tables already have unique provider-event constraints. This phase changes the transactional claim algorithm so those constraints are safe under concurrency.

## Remaining phases

1. Introduce request-level idempotency records for tips, withdrawals, ticket purchases, cart checkout and transaction creation.
2. Add immutable financial ledger invariants and reconciliation jobs.
3. Add atomic stock reservations with expiry and concurrent ticket-capacity tests.
4. Introduce the database outbox for notifications, QR generation, affiliate calculations, reports and webhook follow-up work.
5. Strengthen module boundaries and add Testcontainers tenant-isolation suites across every domain.
