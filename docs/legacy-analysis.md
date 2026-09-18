# Existing Fabklean portal analysis

This analysis is based on the supplied portal screenshots and three sample print files. It records the useful workflow to preserve and the problems the new system must remove. It does not copy Fabklean source code, branding, or proprietary assets.

## Existing workflow observed

| Area | Existing behaviour worth preserving | Improvement in the new system |
|---|---|---|
| Login | Private staff portal | Cleaner login, roles, session/device control, and audit trail |
| New order | Product search, barcode, service-category tabs, customer selector, cart, custom items, tax/HSN, no-print/no-count options | Larger controls, clearer totals, billable quantity separated from physical pieces, open-order warning |
| Quick create | Pickup, customer, business, staff, package, service, batch invoice, refund, recurring pickup | Place these actions in named menus instead of a hidden icon list |
| Processing | In-process/ready lists, delivery grouping, batch actions, staff assignment | Clear filters, status history, overdue alerts, fewer nested action menus |
| Pickup | Today/future/overdue/all pickup views | Manual slot creation, capacity and assignment checks |
| Logistics | Weekly calendar and driver filter | Calendar first; live geo-tracking stays optional |
| Payments | Cash and other modes, transaction reference, partial payment and balance | Validated totals, immutable payment entries, reversal/refund trail |
| Accounts | Ledger, expenses, income, cash/account summary, receivables | Reconciled collections, period filters and exportable reports |
| Inventory | Products, suppliers, purchases, transfers and adjustments | Keep optional for a later phase because laundry services are the priority |
| Administration | Staff, attendance, services, upcharges, price lists, packages, coupons and printers | Role-based settings and safer defaults |

The expanded Dry Clean screenshots also show five catalog groups: Men, Women, Kids, Household and Accessories. The new catalog keeps these as explicit data rather than relying on screen position, so staff can filter a large service list quickly and reports can group items correctly.

## Print files observed

Three formats were supplied:

1. One small garment tag per physical piece. The sample generated seven tags for seven pieces.
2. An A4 invoice showing shop/GST details, order status, payment status, customer, pieces, quantity, rate, total, paid amount, balance, due date and staff.
3. A thermal-style receipt with shop details, order dates, customer, item quantity/rate/value and total.

The old prints have awkward line wrapping, excessive empty A4 space, an order number split across lines on the tag, and browser URL/page headers on the thermal print. The new templates must use printer-specific dimensions and never include browser headers or URLs.

## Confirmed fixes and new features

- A customer can intentionally have many different orders; each order has its own order number and amount.
- A repeated click or network retry with the same client request ID returns the existing order instead of creating another bill.
- Finalizing the same order repeatedly returns the same invoice number. An order cannot acquire two invoices.
- Before a new order is created, open and unpaid orders for that customer are shown for review.
- Every order item stores the rate used at billing time, so later price changes do not change old bills.
- Weight/quantity determines the charge while physical pieces determine how many garment tags are printed.
- The invoice, paid amount, balance and UPI QR are rendered as one image and queued for automatic WhatsApp delivery after finalization. The customer receives media, not a hosted-bill link.
- WhatsApp status and failures are recorded so staff can retry safely without repeatedly sending the same bill.
- Daily, weekly, monthly, custom-period, service-wise and item-wise sales reports are part of the new system.

## Legacy price rule

The previous portal's configured prices are the initial prices for the new catalog. Items that displayed only a name and no amount in the old portal remain inactive/unpriced; the new system does not invent an amount. All catalog prices remain editable by an authorised owner.

Changing a catalog price affects only future orders. For example, a historical invoice may retain a ₹115/kg Wash & Iron rate even when the current legacy catalog shows ₹120/kg, because every order item stores its billed rate as a snapshot.

## Recommended print fields

| Receipt | Garment tag |
|---|---|
| Shop name, address, phones, GSTIN | Full order number |
| Separate order and invoice numbers | Customer short name |
| Created, pickup and delivery times | Service short name |
| Customer name and phone | Piece sequence, for example 3/7 |
| Service, pieces, billable quantity, rate and amount | Delivery date |
| Subtotal, discount, tax, round-off and total | Barcode or QR code |
| Paid, balance and payment mode | Reprint indicator when applicable |
| Staff and terms | No price unless configured |

## Phase boundary

The first checkpoint validates login, dashboard, new-order/POS, customer records, processing lists, reports, data model and billing protections. Production WhatsApp credentials, final printer models/sizes, tax configuration, staff permissions and deployment details are required before go-live.
