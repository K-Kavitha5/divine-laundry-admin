# Divine Laundry Admin — Spring Boot + Thymeleaf

Read **START-HERE-THYMELEAF.md** first. The new website runs at localhost:8080
through `backend/start-demo.cmd` or Maven, not by opening `prototype/index.html`.
The historical instructions below describe the older React/prototype checkpoint.
Their Basic-auth defaults and readiness claims do not apply to the new website.
The current validation status is in **THYMELEAF-VERIFICATION.md**.

## Historical React / prototype checkpoint

Admin-only laundry operations platform inspired by the client's existing workflow and rebuilt with clearer billing, safer invoice generation, garment tagging, and WhatsApp-ready receipts.

## Project layout

- `backend/` - Java 17 + Spring Boot REST API
- `frontend/` - React admin website source
- `prototype/` - dependency-free interactive UI preview
- `docs/` - agreed scope and business rules

## First-release modules

- Secure admin login
- Dashboard
- Customer records
- New order/POS
- Grouped Dry Clean catalog for Men, Women, Kids, Household and Accessories
- Legacy prices preloaded as editable starting prices; unpriced legacy items stay inactive
- Quick actions and custom-item billing fields
- In-process and ready orders
- Per-piece and per-kilogram pricing
- Separate physical-piece and billable-quantity tracking
- Duplicate-order and duplicate-invoice protection
- Payment status and balance tracking
- Receipt and garment-tag data model
- Manual pickup/delivery slots
- Sales-report foundation

## Run the UI prototype

The easiest option on Windows is to extract the latest ZIP and double-click `prototype/index.html`. The preview keeps its JavaScript and CSS beside the HTML so it also works from a local `file:///` address.

Alternatively, run a local server:

```bash
cd divine-laundry-admin
python3 -m http.server 4173
```

Open `http://localhost:4173/prototype/` and sign in with the pre-filled prototype credentials.

The offline prototype now supports a complete safe test flow. Test customers, bills and payments are saved only in that browser with `localStorage`:

1. Sign in with the pre-filled prototype credentials.
2. Open **Customers** and choose **Add customer**.
3. The new customer is automatically selected in **New order**.
4. Select services, adjust quantity/pieces and choose **Create bill & payment**.
5. Record a full or partial payment.
6. Download the invoice PNG or choose **WhatsApp invoice image**.

On a compatible phone, the system share sheet can include the PNG invoice. From a Windows desktop opened with `file:///`, the preview downloads the PNG and opens WhatsApp with the bill text; attach the downloaded PNG before sending. Direct automatic image delivery requires the deployed backend and an official WhatsApp Business provider.

## Run the functional website

The React website now uses the Spring Boot API by default. Start the backend first, then the frontend in a second terminal.

### 1. Backend

```bash
cd backend
mvn spring-boot:run
```

For local development only, the defaults are:

```text
Username: admin
Password: ChangeMe123!
```

Set a new `ADMIN_PASSWORD` before sharing or deploying the system.

### 2. React website

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` and sign in. The website loads customers, the 76-service catalog, active orders, dashboard values and reports from the API.

If a different backend address is used, copy `.env.example` to `.env` and change `VITE_API_URL`.

### Optional sample-data mode

To review the React design without saving real data, set:

```text
VITE_USE_MOCKS=true
```

The dependency-free `prototype/` remains the simplest offline test option. It is not the production database: its records stay only in the browser used for testing.

## Connected in this checkpoint

- Authenticated admin login against Spring Security
- Live customer list and customer creation
- Live legacy service catalog and previous-site starting prices
- Open-order warning when a customer already has active bills
- Order creation with a stable idempotency key
- One-time invoice finalisation
- Idempotent Cash, UPI, Card and bank-transfer payment recording
- Partial-payment balance and `UNPAID`/`PARTIAL`/`PAID` status updates
- Printable invoice and one stable garment tag per physical piece
- Downloadable PNG invoice image for customer sharing
- Backend-generated invoice/payment PNG with a dynamic balance-specific UPI QR
- Official Meta Cloud API media upload and utility-template delivery
- Automatic WhatsApp send after invoice finalisation and each new payment
- Deduplicated WhatsApp retries with visible waiting, sent and failed states
- Dashboard counts and totals from saved data
- Today, this-week and this-month sales reports
- Product/service-wise sales totals, quantities and physical-piece counts

The browser keeps the development login only in memory; it is cleared on logout or when the tab closes.

## Backend environment example

See `backend/.env.example` for the MySQL, web-origin, timezone, admin, UPI and WhatsApp settings required for deployment. The default development database is H2 in MySQL compatibility mode. Follow `docs/whatsapp-automatic-setup.md` before enabling automatic sending.

## Legacy prices

The database and React sample catalog contain the priced items visible in the previous Fabklean website: 76 active services in total, including 58 Dry Clean services. Items that were visible without a price were not guessed and remain inactive until the client confirms a value.

## Current checkpoint

This is a functional website checkpoint, not the production launch. Customer creation, order billing, duplicate-request protection, invoice numbering, payments, invoice/tag printing, sales reports, backend invoice/QR image generation and Meta Cloud API delivery are connected. It intentionally reports `WAITING_FOR_PROVIDER` until the client supplies a verified UPI ID, approved utility template and official WhatsApp Business credentials. Production role management, refund handling, printer calibration, delivery callbacks and deployment still require implementation and testing with the client's accounts and devices.
