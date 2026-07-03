# Email maintenance PR summary

This branch corrects the requested password misunderstanding. No password-change endpoint is included.

Implemented:

- `PATCH /api/auth/email-address` for authenticated users to update their own email address.
- `PATCH /api/admin/users/{userId}/email-address` for temporary admin database maintenance.
- Required cellphone validation for public registration, affiliate registration, and worker creation DTOs.

Important cleanup:

Delete the temporary admin endpoint after the database email records are corrected for Admin, Owner, Worker, Affiliate, and User accounts.
