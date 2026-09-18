# Phase 1 scope

## Confirmed product

The product is private and admin-only. A React website is built first, followed by a Flutter Android application that will reuse the same Spring Boot APIs and MySQL data.

## Core workflow

1. Search for a customer by phone or name.
2. Display any open or unpaid orders before allowing a new one.
3. Select services priced per piece or per kilogram.
   Large categories such as Dry Clean can be filtered by Men, Women, Kids, Household and Accessories.
4. Record billable quantity and physical piece count separately.
5. Choose manual pickup and delivery times.
6. Save a draft or finalize the order.
7. Generate exactly one immutable invoice for the finalized order.
8. Print a customer receipt and one garment tag per physical piece.
9. Send the invoice, paid amount, balance and UPI QR as one image through the official WhatsApp integration.
10. Track processing, payment, delivery, and communication statuses independently.

## Identifier rules

- Order: `SO-YYYY-NNNNNN`
- Invoice: `INV-YYYY-NNNNNN`
- Payment: `PAY-YYYY-XXXXXXXXXXXX`
- Garment tag: `TAG-YYYY-NNNNNN-<item-id>-<piece-sequence>`

One customer may have many orders. One order may have only one finalized invoice. An idempotency key prevents double-clicks and network retries from creating duplicate orders.

## Statuses

- Work: `DRAFT`, `RECEIVED`, `WASHING`, `IRONING`, `CLEANED`, `READY`, `DELIVERED`, `CANCELLED`, `REWORK`
- Payment: `UNPAID`, `PARTIAL`, `PAID`, `REFUNDED`
- WhatsApp: `WAITING_FOR_PROVIDER`, `PENDING`, `SENT`, `DELIVERED`, `FAILED`

## Reports required

- Daily, weekly, monthly, and custom-period sales
- Garment/item-wise sales
- Service-wise sales
- Cash/UPI/card collections
- Unpaid and partially paid invoices
- Expenses and net profit
- Staff-wise billing
- Pickup/delivery performance
- Cancelled/refunded orders
