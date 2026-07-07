# PR 35 AI tips and returnable impact note

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
