# Billing and operations acceptance criteria

## Duplicate protection

1. Double-clicking Create Order with the same client request ID creates one database order.
2. Retrying the same request after a timeout returns that same order number and amount.
3. A staff member may create a legitimate second order for the same customer only with a new request ID; the open-order warning remains visible.
4. Finalizing an order two or more times never changes or creates a second invoice number.

## Pricing and garment tags

1. A 1.39 kg Wash & Iron line at ₹120/kg totals ₹166.80 even when the bag contains seven pieces.
2. The same line generates seven garment tags unless No Print is selected.
3. Updating a service price does not alter saved order-item rates or historical reports.
4. Subtotal, discount, tax, round-off and final total are calculated by the backend.

## Payments

1. Cash, UPI and card entries require a positive amount; UPI/card can store a transaction reference.
2. A partial payment leaves the correct balance and status `PARTIAL`.
3. Total successful payments equal to the invoice total change the status to `PAID`.
4. Refunds/reversals do not delete the original payment trail.
5. Retrying the same payment request ID never records the amount twice.

## WhatsApp invoice delivery

1. Finalization generates one combined invoice/payment image and one deduplicated delivery entry.
2. The automatic message contains the invoice image and balance-specific UPI QR, not a bill-view link.
3. Provider message ID, sent/delivered/failed state, attempts and error are stored.
4. Retrying a failed message does not create a new invoice or a second successful send.
5. Before provider credentials are configured, the queue clearly reports `WAITING_FOR_PROVIDER` and does not pretend the message was sent.
6. Each successful payment creates at most one updated WhatsApp image for that payment number.
7. A zero balance produces a **PAID IN FULL** image without a payment QR.

## Reports

1. Daily, weekly and monthly sales reconcile to finalized, non-cancelled orders for the same period.
2. Collections reconcile to payment entries, not invoice totals.
3. Service/item reports use stored order-item names, rates, quantities and pieces.
4. Unpaid reports reconcile invoice total minus successful payments.
5. Exports use the same filters and totals shown on screen.

## Printing

1. Thermal receipts contain no browser URL, page title or page counter.
2. Order and invoice numbers never wrap in the middle.
3. A4 invoices use the available page cleanly and remain legible when printed in grayscale.
4. Garment tags fit the configured printer and preserve readable barcode/QR quiet zones.
