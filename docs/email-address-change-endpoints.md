# Email address change endpoints

Normal user self-service:

```http
PATCH /api/auth/email-address
Content-Type: application/json
Authorization: Bearer <token>

{
  "emailAddress": "new-email@example.com"
}
```

Temporary admin database maintenance endpoint:

```http
PATCH /api/admin/users/{userId}/email-address
Content-Type: application/json
Authorization: Bearer <admin-token>

{
  "emailAddress": "corrected-user@example.com"
}
```

Use the admin endpoint only while correcting existing database users. Delete it after Admin, Owner, Worker, Affiliate, and User email records are fixed.
