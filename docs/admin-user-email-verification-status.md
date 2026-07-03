# Admin user email verification status endpoint

Use this endpoint when an admin needs to bypass email verification for an existing user account.

```http
PATCH /api/admin/users/{userId}/email-verification-status
Content-Type: application/json
Authorization: Bearer <admin-token>

{
  "emailVerified": true
}
```

Set `emailVerified` to `true` to mark the account verified and allow the user to continue without using the email verification flow. Set it to `false` only when the account must be forced back through verification.

This endpoint is protected by the admin API route and also validates the authenticated actor has the `Admin` privilege before updating the selected user.
