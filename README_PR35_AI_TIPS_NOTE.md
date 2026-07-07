# PR 35 Full King Sparkon AI impact note

## Returnable impact

The hybrid barcode model intentionally changes barcode semantics:

- `Product.productBarcode` is the reusable retail/product barcode.
- `ProductBarcode.unitCode` is the unique physical stock-unit identifier.
- `ProductBarcode.status` remains the returnable claim lifecycle.
- `ProductBarcode.referenceEmail` remains the returnable claim reference.
- `ProductBarcode.availabilityStatus` remains the sale lifecycle.

Returnable claims still use `referenceEmail`, `status`, and product returnable settings. SELL flows now mark scanned `unitCode` records as SOLD and attach the reference email for returnable products.

## Tips impact

The barcode and returnable refactor does not change the core tips model. Tips remain managed by `Tip`, `TipStatus`, `TipService`, and `TipWithdrawalService`.

This PR adds read-only AI tip confirmation:

- `GET /api/tips/{tipId}/ai-confirm` for owners to confirm one tip.
- `GET /api/tips/worker/{workerId}/ai-confirm` for owners to summarize a worker's tips.
- `GET /api/tips/me/ai-confirm` for workers to summarize their own tips.

AI tips confirmation is read-only. It never marks a tip paid, requests withdrawals, changes workers, or mutates payment data.

## Full King Sparkon AI wrap-up

This PR also adds a schema-tolerant Full King Sparkon AI layer:

- users: searches user/profile/business user records when those tables exist.
- jobs and CV/application review: searches job posts, applications, CVs, resumes, and applicant profiles when those tables exist.
- affiliates: searches affiliate, link, click, and referral records when those tables exist.
- affiliate prospects: searches businesses, subscribers, contact inquiries, and newsletter subscribers when those tables exist so affiliates can see potential clients, contact fields, descriptions, and AI niche/talking-angle guidance.
- products, tips, tickets: keeps the earlier AI operations coverage.

The implementation is intentionally read-only and schema tolerant so it does not break existing backend changes while job, affiliate, or subscriber tables evolve.

## Test relevance

Existing barcode model tests remain relevant because they prove reusable product barcodes can map to multiple physical stock units while `unitCode` stays unique.

Existing image upload validation tests remain relevant because the barcode AI flow still rejects empty and unsupported uploads before attempting image decoding.

Local validation still needs to run:

```bash
mvn clean test
mvn clean verify
```
