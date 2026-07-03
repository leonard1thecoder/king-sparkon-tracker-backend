# Temporary admin email endpoint warning

The endpoint below exists only for bootstrap/maintenance while correcting seeded users in the database:

```http
PATCH /api/admin/users/{userId}/email-address
```

Delete this endpoint after the database records for Admin, Owner, Worker, Affiliate, and User accounts have been corrected.

Keep the normal authenticated self-service endpoint:

```http
PATCH /api/auth/email-address
```

The admin endpoint must not remain as a long-term public API surface.
