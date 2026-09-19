# Check the functional website

The offline prototype can now test customer creation, item selection, unique bill creation, payment and WhatsApp-ready invoice PNG sharing in one browser. Use the steps below when you want shared, durable records saved through the backend instead of browser-only test data.

## Required software

- Java 25 or newer
- Maven 3.9 or newer
- Node.js 20 or newer

## Start the backend

Open a terminal inside `divine-laundry-admin/backend` and run:

```bash
mvn spring-boot:run
```

Wait until Spring reports that the application started on port `8080`.

## Start the website

Open a second terminal inside `divine-laundry-admin/frontend` and run:

```bash
npm install
npm run dev
```

Open `http://localhost:5173` in Chrome.

For local testing, sign in with `admin` and `ChangeMe123!`. Change this password before any deployment.

## First functional check

1. Open **Customers** and create a customer.
2. Open **New order** and select that customer.
3. Add one or more services from the previous-site price catalog.
4. Enter billable quantity and physical garment pieces separately.
5. Click **Create invoice** only once.
6. Confirm that the order appears on the Dashboard or In process page.
7. Open **Reports** and switch between Today, This week and This month.
8. Open **Payments**, select the invoiced order, and record a partial or full payment.
9. Use **Print invoice**, **Print garment tags**, or **Download invoice image**.
10. Confirm that invoice finalisation automatically creates a WhatsApp record. It shows `WAITING_FOR_PROVIDER` until the verified UPI ID, approved template and official provider credentials are configured.
11. After provider setup, finalise a small test invoice and confirm that the customer receives one invoice/payment PNG with the full-balance QR.
12. Record a partial payment and confirm that a second PNG arrives automatically with the updated paid amount, balance and remaining-balance QR.

Repeated submission of the same browser request uses the same idempotency key, and invoice finalisation is idempotent. A customer may still have multiple legitimate orders with different items or prices; the website warns the admin about existing open orders before a separate bill is created.

Browser printing and PNG invoice download are available. Allow pop-ups for the website when using the print buttons. Actual WhatsApp sending remains disabled until an official provider, approved utility template and verified UPI ID are configured. Physical tag/receipt sizes must be calibrated with the client's printer.
