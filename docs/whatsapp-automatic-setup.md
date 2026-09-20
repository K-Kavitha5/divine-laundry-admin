# Automatic WhatsApp invoice and UPI QR setup

The backend now creates one PNG containing the invoice, item lines, total, paid amount, balance and a dynamic UPI QR. It uploads that PNG to the official WhatsApp Cloud API and sends it with an approved utility template.

## Automatic triggers

- Finalising an invoice in the backend sends the initial image with a QR for the full balance, whether the action comes from the website or a future mobile app.
- Recording a partial payment sends a new image with the updated paid amount, balance and a QR for only the remaining balance.
- Recording the final payment sends a new image marked **PAID IN FULL** without requesting another payment.
- Invoice and payment-number deduplication prevents the same successful message from being sent twice during retries.

When provider configuration is incomplete, the message remains `WAITING_FOR_PROVIDER`. API errors are stored as `FAILED` with the provider error so an admin can retry safely.

## Meta account requirements

1. Create or use the client's Meta Business account.
2. Add WhatsApp to a Meta app and register the business sending number.
3. Obtain the WhatsApp phone-number ID and a production system-user access token.
4. Select a currently supported Graph API version from the Meta dashboard/documentation.
5. Obtain customer consent/opt-in to receive transactional WhatsApp messages.
6. Create and obtain approval for the utility template below.

Never paste the access token into chat, frontend JavaScript or Git. Store it only as a server environment variable.

## Required utility template

Suggested template name: `laundry_invoice_payment`

- Category: **Utility**
- Header: **Image**
- Language: **English** (`en`) or the exact language code approved in Meta
- Body:

```text
Hello {{1}}, your Divine Laundry invoice {{2}} for order {{3}} is ready.
Total: ₹{{4}}
Paid: ₹{{5}}
Balance: ₹{{6}}
The invoice and payment details are shown in the attached image.
```

Use realistic sample values when submitting the template for approval. The configured template name, language and component order must exactly match the approved Meta template.

## Server environment

Copy `backend/.env.example` to a private deployment environment and set:

```text
UPI_ID=verified-business-upi-id
UPI_PAYEE_NAME=Divine Laundry Trichy
WHATSAPP_ENABLED=true
WHATSAPP_GRAPH_BASE_URL=https://graph.facebook.com
WHATSAPP_GRAPH_API_VERSION=current-supported-version
WHATSAPP_PHONE_NUMBER_ID=meta-phone-number-id
WHATSAPP_ACCESS_TOKEN=production-system-user-token
WHATSAPP_TEMPLATE_NAME=laundry_invoice_payment
WHATSAPP_TEMPLATE_LANGUAGE=en
WHATSAPP_PENDING_TIMEOUT=PT15M
```

Keep `WHATSAPP_ENABLED=false` until the template, phone number, access token and UPI ID have all been verified.

## Payment safety and testing

- Confirm that the displayed payee name and UPI ID belong to the laundry business.
- First test the generated QR with a ₹1 test invoice and an approved test recipient.
- Confirm the amount and order reference shown by more than one common UPI app.
- A UPI QR starts a payment but does not prove that payment succeeded. This version still requires the admin to record the received payment. Automatic reconciliation requires a payment-gateway order and signed webhook integration.
- Do not mark an order paid from a QR scan alone.

## Production verification

## Delivery claim limitation

WhatsApp delivery uses a database-backed claim with a 15-minute stale-`PENDING` recovery window. If the provider accepts a message and the application crashes before saving `SENT`, a later manual retry may send a duplicate after that window. This phase does not implement an outbox or provider reconciliation workflow, so this crash window remains an accepted limitation.

1. Finalise a test invoice and confirm the API status changes to `SENT`.
2. Confirm the customer receives the PNG directly, not a hosted link.
3. Scan the QR and verify payee, amount and invoice reference before paying.
4. Record a partial payment and verify the next image contains the reduced balance.
5. Record the final payment and verify the next image says **PAID IN FULL**.
6. Retry the same invoice/payment request and confirm it does not create a duplicate successful message.

## Official Meta references

- [WhatsApp Business Platform get started](https://developers.facebook.com/documentation/business-messaging/whatsapp/get-started)
- [Media upload and management](https://developers.facebook.com/documentation/business-messaging/whatsapp/business-phone-numbers/media)
- [Template fundamentals](https://developers.facebook.com/documentation/business-messaging/whatsapp/templates/overview)
- [Meta WhatsApp API example repository](https://github.com/fbsamples/whatsapp-api-examples)
