# Thymeleaf milestone 1 — actual validation status

## Passed in this environment

- Java 17 compiler parser: 56 main/test source files, zero syntax errors.
  This was parse-only, not dependency resolution or type checking.
- Node `--check`: new `static/assets/admin.js` parses successfully.
- XML parser: Maven POM is well formed.
- YAML parser: main and demo configuration are valid mappings.
- HTML parser: all 10 template files parse. All five POST forms declare
  Thymeleaf `th:action` for Spring Security CSRF field integration.
- Original V1, V2 and V3 migration bytes match the uploaded archive exactly.
- No actual `.env` credential files in the packaged project.
- Input archive is preserved; work was done in a separate extracted directory.

## Blocked / not verified

- `mvn test`: could not start because Maven is not installed.
- Attempt to contact Maven Central timed out. Dependencies were not downloaded.
- Browser-fixture check: could not start because Playwright's Chromium executable
  is not installed. No browser screenshot or rendered-layout verification passed.
- Spring dependency resolution/type checking, startup, actual Thymeleaf evaluation,
  JPA schema validation, Flyway migrations and database persistence have NOT been
  executed for this milestone.
- No MySQL instance, real printer, Meta account or payment provider was tested.
- No deployment, WhatsApp message, payment request or financial transaction was
  sent to an external service.

## Tests included for the next run

`AdminWebFlowTest` uses a separate in-memory H2 database with automatic WhatsApp
disabled. It covers login/session access, CSRF rejection, normalized customer
creation, duplicate customer handling, order/invoice persistence, repeated order
submission, another order for the same customer, invoice HTML/PNG endpoints,
overpayment rejection and payment replay. The existing domain/service tests are
retained. These tests are authored, not reported as passed.

Run `mvn test` and `mvn package` in `backend` on a computer with JDK 17+ and Maven.
Then follow `START-HERE-THYMELEAF.md` for local demo and fresh-MySQL checks.

## Scope and notable changes

- Added session-login website while retaining the old React/prototype source as
  a reference. Their Basic-auth client is no longer the supported website entry.
- Enabled CSRF; strong admin password required outside local demo.
- Default database is MySQL. Local demo deliberately uses persistent H2, binds
  localhost only and uses a clearly documented demo-only login.
- Customer-locked transactional web order creation with stable request IDs and
  atomic invoice finalization; catalogue rates are resolved server-side.
- Guarded manual totals, per-piece counts and payment precision. The existing
  order-version column remains the payment concurrency conflict safeguard.
- Corrected a missing parenthesis in the uploaded invoice-image renderer, removed
  the 12-line cutoff, added rate/totals details and an explicit missing-QR state.
- After-commit WhatsApp events now enter a new transaction, so queued status
  changes can commit. This does not turn the sender into a durable background
  job or establish exactly-once provider delivery.
- Price correction V4 changes only the catalogue's sofa-seat rate from 190 to
  the screenshot's 199; historical order-item amounts are not changed.

This is a development handoff, not production certification. Live messaging
consent, asynchronous dispatch/recovery, status/report/settings screens, staff
management, audit trails, printer validation, backups and HTTPS deployment remain.
