# Phase 2 production-readiness architecture

## Scope

This phase preserves the modular-monolith deployment model and existing public URLs while adding integrity controls around financial and scarce-resource workflows.

## Request idempotency

Authenticated mutation endpoints for tips, withdrawals, transactions, Tuck Shop checkout, embedded checkout and ticket purchases require `Idempotency-Key`.

Records are scoped by operation, authenticated username and key. The authenticated actor service supplies the tenant context; frontend-supplied business IDs do not participate in authorization. A repeated key with the same request payload replays the stored response. A repeated key with a different payload returns `409 IDEMPOTENCY_CONFLICT`.

## Double-entry financial journal

Posted business-account entries are mirrored into immutable journals with exactly one debit and one credit. The journal source tuple is unique per business and the immutable hash covers business, source, amount, direction, counterparty and currency.

Owner endpoints:

- `GET /api/business-account/reconciliation`
- `POST /api/business-account/reconciliation`

The POST endpoint repairs missing journals from posted legacy entries and then verifies balance and hashes.

## Inventory reservations

Website and embedded-cart payments reserve stock under pessimistic product locks. Stock is removed from available inventory at reservation time. Explicit barcode units move from `AVAILABLE` to `RESERVED`, then to `SOLD` only after verified provider success. Failed, cancelled or expired payments restore quantities and barcode availability.

## Ticket reservations

Ticket capacity is reserved under a pessimistic ticket-type lock. Purchased ticket rows remain `PENDING_PAYMENT` and do not receive active QR values until a verified payment webhook succeeds. Failed or expired reservations release capacity. Gate verification locks the ticket row and rejects duplicate or cross-business verification.

## Database outbox

Email, notifications, QR generation, affiliate commission calculations and report generation are published as outbox records in the same database transaction as the initiating domain change. Workers claim events in bounded batches, retry with backoff and move exhausted events to `DEAD_LETTER`.

No email address, token, provider secret or full payment payload is written to application logs.

## Migration

`V20260714_02__production_integrity_foundations.sql` creates idempotency records, immutable journals, stock and ticket reservations, outbox events, generated QR/report records, tenant columns and lookup indexes. Existing applied migrations are unchanged.

## Validation gates

- Java 25 compile and Maven verify
- Flyway migration validation on H2/PostgreSQL-compatible test profiles
- Docker image build and inspection
- duplicate-request, cross-tenant, concurrent stock, concurrent ticket and duplicate QR verification tests
