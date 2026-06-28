# Ticket Management Backend

This package implements the King Sparkon Tracker event ticket backend.

## Main API routes

- `GET /api/v1/tickets/events`
- `GET /api/v1/tickets/events/{eventId}`
- `POST /api/v1/tickets/events`
- `PATCH /api/v1/tickets/events/{eventId}`
- `POST /api/v1/tickets/purchase`
- `GET /api/v1/tickets/my-tickets?userId=...`
- `POST /api/v1/tickets/verify/qr`
- `POST /api/v1/tickets/verify/reference`
- `GET /api/v1/tickets/owner/dashboard?ownerId=...`
- `GET /api/v1/tickets/owner/events?ownerId=...`
- `POST /api/v1/tickets/owner/withdrawals`
- `GET /api/v1/tickets/owner/withdrawals?ownerId=...`
- `GET /api/v1/tickets/observability/entries?ownerId=...`

## Fee policy

Ticket withdrawals use the same transparent payout pattern as tips: gross amount, service fee, net amount, and status are stored and returned.

Default ticket withdrawal service fee: `5.00%`.

Configure with:

```properties
app.tickets.withdrawal-fee-percent=${TICKETS_WITHDRAWAL_FEE_PERCENT:5.00}
app.tickets.withdrawal-minimum-zar=${TICKETS_WITHDRAWAL_MINIMUM_ZAR:100.00}
```

## Production TODO

The payment record is currently a typed placeholder. Wire `TicketPayment` creation to the real Stripe/Ozow/Yoco checkout provider when the payment flow is ready.
