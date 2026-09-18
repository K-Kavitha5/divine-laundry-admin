# Information required before production development

## Business and branding

- Final shop name, logo, address, phone numbers, email and GSTIN
- Approved green/white colour palette or alternative brand colours
- Receipt footer, terms and customer-care wording
- Tamil language requirement, if any

## Billing

- Tax-inclusive or tax-exclusive pricing rules
- Required HSN/SAC codes and GST split rules
- Discount, upcharge, express-service and round-off rules
- Order/invoice number starting sequence
- Whether an invoice may be edited, cancelled or replaced after finalization

## Services and operations

- Confirm the imported service/category/price list from the previous portal; unpriced legacy items remain inactive
- Which items are priced per kg, piece, pair, seat or square foot
- Final work statuses and allowed status movement
- Pickup/delivery slot duration, business hours and blackout days
- Staff roles and which actions each role may perform

## Printing

- Thermal printer brand/model and paper width, usually 58 mm or 80 mm
- Garment-tag printer brand/model and exact label dimensions
- Number of receipt copies and whether price is shown on garment tags
- Barcode or QR preference and scanner model

## WhatsApp

- Meta Business/WhatsApp Business account ownership
- Sending phone number, phone-number ID and approved image-header utility template
- Production system-user access token stored only in the backend environment
- Message language and wording
- Verified business UPI ID and exact payee name for the payment QR
- Automatic sending is currently selected for invoice finalization and each payment update
- Failure recipient and manual retry process

## Existing data migration

- Export of customers, open orders, unpaid balances, services/prices and payments
- Decision on whether completed historical orders must be searchable
- Reconciliation date and signed opening cash/account balances

Do not enter production data until a backup of the old system export has been verified and a sample migration has reconciled customer balances.
