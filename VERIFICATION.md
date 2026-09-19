# Historical checkpoint verification record

These notes came with the uploaded React/prototype checkpoint. They are not
evidence that the new Thymeleaf website builds or runs. For current checks,
read **THYMELEAF-VERIFICATION.md**.

## Completed checks

- Dependency-free prototype JavaScript passed Node syntax validation.
- Prototype render smoke test passed this path: login -> dashboard -> new order -> grouped Dry Clean catalog -> custom item -> quick actions -> reports.
- Prototype HTML entry point and referenced assets were checked.
- CSS braces and parentheses are balanced.
- `application.yml` parsed successfully.
- Maven `pom.xml` parsed successfully as XML.
- Uploaded garment-tag, A4 invoice and thermal-receipt PDFs were rendered and visually reviewed.
- Legacy catalog validation passed with 76 configured services, including 58 priced Dry Clean services across five groups.
- Frontend service IDs/codes and database seed codes were checked for duplicates, and representative legacy prices were matched across both catalogs.
- API-client smoke tests passed for Basic authentication, multi-status order loading and matching header/body idempotency keys.
- Payment API smoke tests passed for matching request/header idempotency and WhatsApp invoice-queue routing.
- Printable invoice and garment-tag helpers passed JavaScript syntax validation plus an escaped-content render smoke test; the PNG invoice helper passed syntax validation.
- Payment, garment-tag, invoice-asset and WhatsApp tables plus the payment-idempotency migration were checked for structural presence.
- React source delimiter validation passed after connecting login, customers, orders and reports to the API.
- Dashboard repository queries and frontend response fields were checked against the connected API contract.
- Automatic invoice-finalisation and payment-recorded events were added so website and future app requests use the same server-side WhatsApp flow.
- WhatsApp media upload and approved image-template payloads have an isolated local HTTP integration test, including Indian phone normalization and provider-message ID capture.
- The production WhatsApp HTTP client compiled with Java 25 and passed a local two-step media-upload/template-send smoke test against an in-memory HTTP server.
- Invoice/payment PNG generation has a test for image dimensions and an amount-locked UPI URI; zero-balance rendering omits the payment QR and shows paid status.
- WhatsApp invoice and payment messages use distinct deterministic deduplication keys, and successful retries return the original message instead of sending another copy.
- WhatsApp/UPI environment settings were checked to ensure the access token and verified UPI ID are not stored in frontend source.
- Project source was scanned for trailing whitespace, unfinished work markers and copied customer details from the screenshots.

## Not yet verified

- A full `npm run build` was not possible in this workspace because the React/Vite packages were not locally installed and package-registry access was unavailable. The API client and dependency-free prototype passed executable Node checks; the React build must still be run on the developer machine.
- A full Maven test run was not possible because Maven and the Spring/ZXing dependency cache were unavailable. The new JUnit tests are included and must be run on the developer machine.
- Browser-based screenshot testing of the local preview was blocked by the remote preview browser's local-address policy; the dependency-free render smoke test was used instead.

Run both normal builds in a development environment with Maven and npm before deployment. Production WhatsApp, UPI QR, printer and MySQL integration tests require the client's actual accounts and devices. A QR opens a payment request; it does not by itself confirm bank settlement, so payments remain admin-recorded until a gateway webhook is added.
