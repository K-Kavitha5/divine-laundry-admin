# Laundry Admin Management System — GitHub Copilot Instructions

## 1. Project Overview

Build a production-ready **Laundry Admin Management System** to replace the existing laundry management workflow.

This is an **admin-focused web application**. It must be a real working business application, not a demo, static HTML website, or simple contact/WhatsApp form.

### Primary goal

The system should allow laundry staff/admins to manage:

* Customers
* Laundry orders
* Individual laundry items
* Item tagging
* Bill/order numbers
* Manual booking/slot scheduling
* Order status
* Billing and invoices
* Payments
* QR payment information
* WhatsApp invoice sharing
* Sales reports
* Weekly/monthly reports
* Product/item-wise reports
* Customer history
* Search and filtering

The application should be simple enough for daily staff usage while having a professional, modern UI.

---

# 2. Existing System Reference

The old laundry management system was based around:

https://app.fabklean.com/outlet/

The new application should replace the old workflow with our own implementation.

Do **not** copy proprietary source code, credentials, branding, or implementation details from the old system.

Use the old system only as a reference for understanding the type of laundry business workflow required.

The new application must have its own:

* UI
* database
* backend
* authentication
* business logic
* invoice generation
* reports
* WhatsApp integration

---

# 3. Application Type

Build this as:

**Spring Boot + Thymeleaf + MySQL**

Preferred architecture:

```text
Browser
   ↓
Thymeleaf UI
   ↓
Spring MVC Controllers
   ↓
Service Layer
   ↓
Repository Layer
   ↓
MySQL
```

External integrations:

```text
Spring Boot
   ├── WhatsApp Business / Meta Graph API
   ├── PDF generation
   ├── QR payment generation/display
   └── Optional email services
```

Do not introduce unnecessary microservices.

For this laundry application, a **well-structured modular monolith** is preferred unless there is a strong technical reason to separate services.

---

# 4. Technology Stack

Use modern stable versions compatible with the project.

Preferred stack:

* Java
* Spring Boot
* Spring MVC
* Spring Data JPA
* Hibernate
* MySQL
* Thymeleaf
* Spring Security
* Bootstrap
* JavaScript where required
* HTML5
* CSS3
* Maven
* Lombok only when it genuinely improves readability
* Bean Validation

Potential libraries:

* PDF generation library
* QR code generation library
* WhatsApp Meta Graph API integration
* Jackson for JSON
* Apache Commons where useful

Do not add dependencies unnecessarily.

Before adding a dependency, check whether the functionality can reasonably be implemented using existing Spring/Java functionality.

---

# 5. Architecture Rules

Use clear separation of responsibilities.

Recommended package structure:

```text
com.example.laundry
│
├── config
├── controller
├── dto
├── entity
├── repository
├── service
├── service.impl
├── security
├── exception
├── validation
├── util
├── integration
│   └── whatsapp
└── mapper
```

Resources:

```text
src/main/resources/
│
├── templates/
│   ├── login.html
│   ├── dashboard.html
│   ├── customers/
│   ├── orders/
│   ├── items/
│   ├── billing/
│   ├── reports/
│   └── fragments/
│
├── static/
│   ├── css/
│   ├── js/
│   └── images/
│
└── application.properties
```

Keep controllers thin.

Controllers should:

* Receive requests
* Validate input
* Call services
* Prepare model attributes
* Return views/redirects

Business logic belongs in services.

Database operations belong in repositories.

Do not put business logic directly inside Thymeleaf templates.

---

# 6. Coding Principles

Always prefer:

* Clean code
* Simple code
* Readable names
* Small methods
* Single responsibility
* Proper exception handling
* DTOs where appropriate
* Validation
* Transaction boundaries
* Meaningful logging

Avoid:

* Huge controller classes
* Huge service methods
* Duplicate code
* Hardcoded credentials
* Hardcoded API tokens
* Hardcoded customer data
* Hardcoded prices
* Hardcoded WhatsApp numbers
* SQL embedded directly in controllers
* Business logic in HTML
* Unnecessary complexity

Do not rewrite working parts of the application unnecessarily.

When modifying existing code, preserve existing functionality unless the requirement explicitly changes it.

---

# 7. Security

The application is admin-focused.

Use Spring Security.

Authentication should be server-side and secure.

Requirements:

* Login page
* Secure password storage
* Password hashing
* Session-based authentication
* Logout
* Protected admin pages
* Unauthorized users redirected to login
* CSRF protection where appropriate
* Role-based authorization if multiple admin roles are introduced

Never store plain-text passwords.

Never commit:

```text
password
API keys
access tokens
client secrets
database passwords
private keys
WhatsApp tokens
```

Use environment variables or secure configuration.

Example:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

---

# 8. Admin Dashboard

The dashboard should be the main landing page after login.

Show useful business information such as:

* Today's orders
* Pending orders
* Ready orders
* Delivered orders
* Today's sales
* Pending payments
* Total customers
* Orders awaiting processing

Dashboard should be visually clean and easy to understand.

Use cards, tables, filters and simple charts where appropriate.

Avoid excessive animations.

The UI should prioritize operational usability.

---

# 9. Customer Management

Admin should be able to:

* Add customer
* Edit customer
* Search customer
* View customer
* View customer order history
* View customer contact information
* View customer payment/order history

Suggested fields:

```text
Customer
---------
id
name
mobileNumber
alternateNumber
email
address
city
notes
createdAt
updatedAt
active
```

Mobile number should be validated.

Do not allow accidental duplicate customers where the business rules identify the same customer.

---

# 10. Laundry Order Management

An order represents one laundry transaction.

Suggested fields:

```text
LaundryOrder
------------
id
orderNumber
customer
bookingDate
pickupDate
expectedDeliveryDate
actualDeliveryDate
status
paymentStatus
subtotal
discount
tax
totalAmount
paidAmount
balanceAmount
notes
createdAt
updatedAt
```

Order number must be unique.

Do not use database ID directly as the customer-facing bill/order number.

Example:

```text
LND-2026-000001
```

The exact numbering format may be changed later, but it must remain unique and human-readable.

---

# 11. Order Status

Use a clear lifecycle.

Suggested statuses:

```text
BOOKED
RECEIVED
PROCESSING
READY
DELIVERED
CANCELLED
```

Do not allow invalid status transitions without a deliberate business rule.

For example:

```text
BOOKED
  ↓
RECEIVED
  ↓
PROCESSING
  ↓
READY
  ↓
DELIVERED
```

Cancelled orders should be handled separately.

Status should be visible in the order list and order details.

Use badges/colors carefully to make status easy to recognize.

---

# 12. Laundry Item Management

Each order can contain multiple laundry items.

Example:

```text
Order #LND-2026-000001

Shirt       3 × ₹80
Pant        2 × ₹100
Bedsheet    1 × ₹180
```

Suggested entity:

```text
LaundryItem
-----------
id
order
itemType
description
quantity
unitPrice
totalPrice
tagNumber
specialInstructions
itemStatus
createdAt
updatedAt
```

Each item should be associated with an order.

---

# 13. Item Tagging

Item tagging is an important requirement.

The system should support assigning a unique tag/reference to laundry items.

Example:

```text
Order: LND-2026-000125

Items:

TAG-000451 → Shirt
TAG-000452 → Pant
TAG-000453 → Bedsheet
```

Tag numbers must be unique.

The admin should be able to identify items quickly from the tag.

Tag information should be printable where required.

Avoid duplicate tag numbers.

---

# 14. Bill / Invoice Number

Every order should have a unique bill/order number.

Example:

```text
LND-2026-000125
```

The number should appear on:

* Order page
* Invoice
* Printable bill
* WhatsApp invoice
* Reports
* Order history

Do not expose internal database IDs unnecessarily.

---

# 15. Manual Booking / Slot Management

The system must support manual booking.

Admin should be able to create an order manually.

Possible workflow:

```text
Select customer
      ↓
Select booking date
      ↓
Select pickup/delivery information
      ↓
Add laundry items
      ↓
Calculate amount
      ↓
Record payment
      ↓
Generate order number
      ↓
Save order
```

If slot management is enabled, prevent accidental double booking according to configured capacity.

Do not assume unlimited capacity.

The capacity should be configurable if slot functionality is implemented.

---

# 16. Pricing

Prices must not be hardcoded in controllers or HTML.

Create a pricing structure that allows administrators to configure laundry item prices.

Example:

```text
Shirt       ₹80
Pant        ₹100
Bedsheet    ₹180
Blanket     ₹250
```

Prices may change in the future.

The order should preserve the price used at the time of purchase.

If the master price later changes, old invoices must NOT change.

Therefore:

```text
Item Master Price
        ↓
Copied to Order Item
        ↓
Historical order keeps original price
```

---

# 17. Billing Calculation

Calculate:

```text
Subtotal
- Discount
+ Tax (if enabled)
= Total
```

Then:

```text
Total
- Paid Amount
= Balance Amount
```

Use `BigDecimal` for monetary calculations.

Never use `double` or `float` for money.

Example:

```java
BigDecimal
```

Use proper rounding rules.

---

# 18. Payment Management

Support payment states such as:

```text
UNPAID
PARTIALLY_PAID
PAID
```

The system should show:

* Total amount
* Paid amount
* Balance amount
* Payment status
* Payment method

Possible payment methods:

```text
CASH
UPI
CARD
OTHER
```

Payment functionality should be designed so additional methods can be added later.

---

# 19. QR Payment

The invoice/billing screen should show QR payment information when required.

The QR should contain the configured UPI/payment information.

Do not hardcode the payment identifier throughout the application.

Keep it configurable.

For example:

```text
UPI ID
Business Name
Payment Note
```

The QR should be usable from the generated invoice where appropriate.

Never expose secret payment credentials.

---

# 20. Invoice Generation

The system should generate a professional invoice/bill.

Invoice should include:

```text
Laundry Business Name
Address
Phone
Invoice / Order Number
Date
Customer Name
Customer Phone

Items
-------------------------------
Item | Qty | Price | Total
-------------------------------

Subtotal
Discount
Tax
Grand Total
Paid
Balance

Payment information
QR code

Thank you message
```

Invoice should be suitable for:

* Printing
* PDF generation
* WhatsApp sharing
* Customer records

Use a clean professional layout.

---

# 21. PDF Generation

PDF generation must happen server-side.

Do not generate a fake PDF containing only an HTML screenshot.

The PDF should contain actual invoice information.

Use a maintainable PDF library.

Invoice generation should be implemented as a service.

Example:

```text
InvoiceService
PdfInvoiceService
```

Keep PDF generation separate from controllers.

---

# 22. WhatsApp Integration

The system should support sending invoice information through WhatsApp.

Preferred integration:

**Meta WhatsApp Business Platform / Graph API**

Do not use unofficial WhatsApp automation or browser scraping.

Configuration must come from environment variables.

Example:

```text
WHATSAPP_ACCESS_TOKEN
WHATSAPP_PHONE_NUMBER_ID
WHATSAPP_BUSINESS_ACCOUNT_ID
WHATSAPP_API_VERSION
```

Never commit these values.

---

# 23. WhatsApp Invoice Workflow

After an order is completed/saved:

```text
Order created
      ↓
Invoice generated
      ↓
PDF/image prepared
      ↓
WhatsApp API request
      ↓
Customer receives invoice
```

The system should clearly show whether sending succeeded or failed.

Do not make the entire order transaction fail simply because WhatsApp temporarily fails.

For example:

```text
Order saved successfully
WhatsApp sending failed
```

should be represented as two separate states.

Allow admin to retry WhatsApp sending.

---

# 24. WhatsApp Error Handling

Handle:

* Invalid phone number
* Expired access token
* API timeout
* API rate limit
* Invalid media
* Network failure
* Template errors
* Authentication errors

Log useful technical information without exposing secrets.

Do not show raw access tokens or sensitive API responses to the admin.

User-friendly message:

```text
Invoice created successfully, but WhatsApp delivery failed.
Please retry.
```

---

# 25. WhatsApp Templates

If Meta requires an approved WhatsApp template for the selected messaging workflow, design the integration around templates.

Do not assume arbitrary business-initiated messages can always be sent.

Template name, language and variables should be configurable.

Example conceptual template:

```text
Hello {{customerName}},

Your laundry order {{orderNumber}} is ready.

Total: ₹{{total}}
Balance: ₹{{balance}}

Thank you.
```

Actual template requirements must follow the current Meta WhatsApp Business API rules.

---

# 26. Reports

The application must provide useful business reports.

Required reports:

### Daily

* Number of orders
* Sales
* Amount collected
* Outstanding balance

### Weekly

* Total orders
* Total sales
* Payment collection
* Outstanding amount

### Monthly

* Total orders
* Total sales
* Paid amount
* Outstanding amount

### Product / Item-wise

Example:

```text
Shirt       125
Pant         98
Bedsheet     45
Blanket      21
```

Also show revenue by item where useful.

---

# 27. Report Filters

Reports should support filters such as:

```text
From Date
To Date
Order Status
Payment Status
Item Type
Customer
```

Use efficient database queries.

Do not load thousands of records into Java just to calculate simple database aggregates.

Prefer database aggregation for large datasets.

---

# 28. Search

Provide fast search for:

* Order number
* Customer name
* Mobile number
* Tag number

Search should be case-insensitive where appropriate.

Support pagination for large datasets.

---

# 29. Pagination

Do not display every record on a single page.

Use pagination for:

* Customers
* Orders
* Items
* Reports where appropriate

Example:

```text
Previous  1  2  3  4  Next
```

---

# 30. UI / UX Requirements

The UI should look like a professional business application.

Design principles:

* Clean
* Modern
* Responsive
* Fast
* Easy to operate
* Mobile-friendly
* Desktop-friendly
* Clear typography
* Consistent spacing
* Consistent buttons
* Consistent forms
* Clear status badges

The application should not look like a basic college project.

Use Bootstrap where useful, but customize the design rather than relying on completely default Bootstrap styling.

---

# 31. Main Pages

Expected pages include:

```text
/login

/dashboard

/customers
/customers/new
/customers/{id}
/customers/{id}/edit

/orders
/orders/new
/orders/{id}
/orders/{id}/edit

/items

/billing

/invoices/{id}
/invoices/{id}/pdf

/reports
/reports/daily
/reports/weekly
/reports/monthly
/reports/items

/settings
```

Exact URLs can change if there is a strong architectural reason.

---

# 32. Thymeleaf Rules

Use reusable fragments.

Example:

```text
templates/fragments/
    navbar.html
    sidebar.html
    footer.html
    alerts.html
```

Avoid duplicating the same navbar/sidebar HTML across every page.

Use Thymeleaf correctly:

```html
th:if
th:each
th:text
th:href
th:action
th:object
th:field
```

Forms should use proper validation.

Show useful validation errors next to fields.

---

# 33. Form Validation

Validate at the backend even if frontend JavaScript validation exists.

Examples:

```text
Customer name → required
Mobile number → valid format
Quantity → positive
Price → non-negative
Date → valid
Payment amount → non-negative
```

Never trust client-side validation alone.

Use:

```java
jakarta.validation
```

where appropriate.

---

# 34. Exception Handling

Create centralized exception handling.

Use:

```text
@ControllerAdvice
```

where appropriate.

Create meaningful exceptions such as:

```text
CustomerNotFoundException
OrderNotFoundException
DuplicateOrderException
InvalidOrderStatusException
InvoiceGenerationException
WhatsAppIntegrationException
```

Do not expose stack traces to users.

---

# 35. Logging

Use structured, useful logging.

Log:

* Important business events
* Integration failures
* Exceptions
* Authentication events where appropriate

Never log:

* Passwords
* Access tokens
* API secrets
* Full payment credentials

---

# 36. Database Design

Use normalized relational design.

Suggested core tables:

```text
users
customers
orders
order_items
item_types
payments
invoice
whatsapp_messages
settings
```

Additional tables can be introduced when justified.

Use proper:

* Primary keys
* Foreign keys
* Unique constraints
* Indexes
* Timestamps

Important indexes may include:

```text
orders.order_number
customers.mobile_number
order_items.tag_number
orders.booking_date
orders.status
```

Do not over-index without reason.

---

# 37. Entity Relationships

Conceptually:

```text
Customer
   │
   └──< Orders
            │
            ├──< OrderItems
            │
            ├──< Payments
            │
            └── Invoice
```

One customer can have many orders.

One order can have many items.

One order can have multiple payments if partial payments are supported.

---

# 38. Audit Information

Where useful, store:

```text
createdAt
updatedAt
createdBy
updatedBy
```

This helps determine who created or changed an order.

Do not introduce a complex auditing framework unless it provides real value.

---

# 39. Transaction Management

Use transactions around operations that must succeed together.

Example:

```text
Create Order
  + Order Items
  + Initial Payment
```

should be handled consistently.

If an operation fails, do not leave partially saved business data.

However, external services such as WhatsApp should not unnecessarily participate in the database transaction.

Preferred:

```text
DB transaction
    ↓
Commit
    ↓
WhatsApp integration
```

---

# 40. WhatsApp and Database Separation

Never make this design:

```text
Save order
   ↓
Wait for WhatsApp
   ↓
Only then commit order
```

Instead:

```text
Save order
   ↓
Commit
   ↓
Attempt WhatsApp
```

Store WhatsApp delivery status separately.

Example:

```text
PENDING
SENT
FAILED
```

---

# 41. Configuration

Application-specific settings should be configurable.

Examples:

```text
Business name
Business address
Business phone
UPI ID
Invoice prefix
Tax percentage
WhatsApp configuration
Order capacity
```

Do not hardcode these values into Java classes.

Environment-specific secrets belong in environment variables.

---

# 42. Development Environment

The application should run locally using:

```text
Java
Maven
MySQL
```

Recommended:

```text
mvn spring-boot:run
```

Database configuration should be straightforward.

Provide a README explaining:

1. Requirements
2. Database creation
3. Environment variables
4. Running the application
5. Login setup
6. WhatsApp configuration
7. PDF configuration

---

# 43. Git Rules

Never commit:

```text
.env
application-local.properties
real passwords
API tokens
private credentials
database dumps containing customer information
```

Use:

```text
.gitignore
```

appropriately.

Before suggesting a commit, check that secrets are not being added.

---

# 44. Production Readiness

Before considering a module complete, check:

### Backend

* Validation
* Exception handling
* Transactions
* Security
* Logging
* Database constraints
* Pagination
* Proper HTTP responses where APIs exist

### Frontend

* Responsive UI
* Validation messages
* Loading states where required
* Empty states
* Error states
* Confirmation dialogs for destructive actions

### Business

* Correct billing
* Correct payment calculation
* Correct order status
* Unique order number
* Unique tags
* Correct invoice
* Correct report totals

---

# 45. Important Business Rules

Always preserve these rules:

1. Every order must have a unique order number.
2. Every laundry item tag must be unique.
3. Money must use BigDecimal.
4. Historical order prices must not change when master pricing changes.
5. Customer order history must remain accessible.
6. Deleted data should not accidentally destroy historical billing information.
7. WhatsApp failure must not automatically delete or roll back a successfully saved order.
8. Invoice totals must be calculated server-side.
9. Admin-only pages must be protected.
10. Credentials must never be committed.
11. Reports must use correct date boundaries.
12. Cancelled orders must be handled explicitly.
13. Payment balance must never become negative unless the business explicitly supports refunds/credits.
14. PDF invoices must reflect the stored order values.
15. Old invoices must remain historically accurate.

---

# 46. Data Deletion

Avoid hard deletion of important business records.

For customers/orders/invoices, prefer an appropriate inactive/cancelled/archive mechanism when historical information must be preserved.

Never allow an admin action to accidentally remove financial history.

If hard deletion is truly required, enforce dependency checks.

---

# 47. Mobile Usability

Laundry staff may use the system from a phone.

Therefore:

* Buttons must be touch-friendly.
* Forms should work on small screens.
* Tables should be responsive.
* Important information should remain visible.
* Avoid tiny text.
* Avoid hover-only functionality.
* Keep order creation efficient.

---

# 48. Accessibility

Use:

* Proper labels
* Semantic HTML
* Keyboard-friendly forms
* Accessible buttons
* Clear validation messages
* Adequate contrast
* Meaningful page titles

Do not rely only on color to communicate status.

---

# 49. Performance

Avoid unnecessary database calls.

Watch for:

```text
N + 1 query problems
```

Use appropriate:

* Fetch strategies
* Projections
* DTO queries
* Pagination
* Database indexes

Do not use eager loading everywhere as a quick fix.

---

# 50. API Design

If REST APIs are needed internally or for future mobile integration, use:

```text
/api/customers
/api/orders
/api/items
/api/payments
/api/reports
/api/invoices
```

Use appropriate HTTP methods:

```text
GET
POST
PUT/PATCH
DELETE
```

Do not create APIs simply because REST exists.

Thymeleaf MVC is the primary UI architecture.

---

# 51. Future Mobile App Compatibility

The current application is primarily a Thymeleaf admin web application.

However, structure services and DTOs cleanly so a future Flutter/mobile application can consume APIs without rewriting all business logic.

Business logic should therefore remain in service classes rather than being tied to Thymeleaf controllers.

---

# 52. Testing

Add tests for important business logic.

Prioritize:

* Billing calculation
* Payment calculation
* Order number generation
* Tag number generation
* Status transitions
* Customer creation
* Order creation
* Invoice generation
* Report calculations

Use:

```text
JUnit
Mockito
Spring Boot Test
```

where appropriate.

Do not create meaningless tests just to increase coverage percentage.

---

# 53. Test Data

Development test data may be used locally.

Never commit real customer information.

Use clearly fake data:

```text
Customer: Test Customer
Phone: 9000000000
```

Do not use real customer phone numbers in GitHub examples.

---

# 54. GitHub Copilot Behavior

When working on this project:

### First

Understand the existing code before modifying it.

### Second

Identify:

* Existing architecture
* Existing entities
* Existing controllers
* Existing services
* Existing database schema

### Third

Make the smallest clean change that solves the requirement.

Do not rewrite the entire project unless explicitly requested.

---

# 55. When User Requests a New Feature

Follow this process:

```text
1. Understand requirement
2. Inspect existing implementation
3. Identify affected layers
4. Design database changes if necessary
5. Implement entity/DTO
6. Implement repository
7. Implement service
8. Implement controller
9. Implement Thymeleaf UI
10. Add validation
11. Add exception handling
12. Add tests
13. Update README if necessary
```

Do not skip directly to HTML if backend support is required.

---

# 56. When Fixing Bugs

Do not blindly rewrite code.

Follow:

```text
1. Reproduce/understand problem
2. Identify root cause
3. Check logs/error messages
4. Trace controller → service → repository
5. Fix root cause
6. Preserve unrelated functionality
7. Add regression test
```

Explain the root cause briefly before making a large architectural change.

---

# 57. UI Design Direction

The application should feel like a modern SaaS/admin dashboard.

Preferred visual direction:

* Professional
* Minimal
* Clean
* Premium but practical
* White/light surfaces where appropriate
* Soft shadows
* Rounded cards
* Clear navigation
* Strong hierarchy
* Consistent spacing

Do not overuse:

* Gradients
* Glassmorphism
* Large animations
* Decorative elements
* Huge icons

This is a business management system first.

---

# 58. Sidebar Navigation

Suggested:

```text
Dashboard

Orders
  ├── All Orders
  ├── New Order
  └── Pending

Customers

Billing

Invoices

Reports
  ├── Daily
  ├── Weekly
  ├── Monthly
  └── Item-wise

Settings

Logout
```

Navigation can be adjusted as the application grows.

---

# 59. Order Creation UX

Order creation should be one of the fastest workflows.

Recommended:

```text
Customer
   ↓
Order Details
   ↓
Add Items
   ↓
Automatic Price Calculation
   ↓
Payment
   ↓
Review
   ↓
Create Order
   ↓
Invoice
   ↓
WhatsApp
```

Do not make staff enter the same information multiple times.

---

# 60. Confirmation Before Destructive Actions

For:

* Cancel order
* Delete customer
* Delete item
* Remove payment

show a confirmation.

Example:

```text
Are you sure you want to cancel order LND-2026-000125?
```

Do not silently execute destructive actions.

---

# 61. Empty States

Every list should have a useful empty state.

Example:

```text
No orders found.

Create your first laundry order to get started.
```

Do not show a completely blank page.

---

# 62. Error States

When something fails:

* Explain what happened.
* Give the user a possible next action.
* Do not expose stack traces.

Example:

```text
Unable to generate invoice.

Please try again. If the problem continues, contact the administrator.
```

---

# 63. Date and Time

Use a consistent timezone configured for the business.

Do not mix server timezone and business timezone accidentally.

Store timestamps consistently and format them appropriately for the admin UI.

---

# 64. Currency

The initial application is intended for India.

Use:

```text
INR / ₹
```

for the current implementation.

Do not hardcode the currency symbol in dozens of templates.

Create a reusable formatting approach.

---

# 65. Internationalization

Full multi-language support is not required initially.

Keep text centralized enough that future internationalization is possible.

Do not over-engineer this in the first version.

---

# 66. Security Against Common Web Issues

Consider:

* SQL injection
* XSS
* CSRF
* Session fixation
* Broken access control
* Insecure direct object references
* File upload validation
* Path traversal
* Malicious PDF/file input

Use Spring's built-in security mechanisms where possible.

---

# 67. File Handling

If invoice PDFs are generated:

* Use controlled storage
* Do not expose arbitrary filesystem paths
* Validate filenames
* Prevent path traversal
* Do not allow users to access another customer's files by changing an ID without authorization

---

# 68. Secrets and Environment Variables

Use environment variables for all external credentials.

Example:

```properties
WHATSAPP_ACCESS_TOKEN=${WHATSAPP_ACCESS_TOKEN}
WHATSAPP_PHONE_NUMBER_ID=${WHATSAPP_PHONE_NUMBER_ID}
WHATSAPP_API_VERSION=${WHATSAPP_API_VERSION}
```

Never write real values into source code.

If Copilot detects a secret in code, recommend moving it to environment configuration.

---

# 69. Database Migration

For production, prefer a proper database migration strategy such as:

```text
Flyway
```

if the project adopts database versioning.

Do not repeatedly destroy/recreate production tables during development.

---

# 70. Documentation

Maintain a useful README containing:

```text
Project Overview
Features
Technology Stack
Architecture
Database Setup
Environment Variables
Local Setup
Running Application
Admin Login Setup
WhatsApp Setup
Invoice/PDF Setup
Testing
Deployment
```

Keep README aligned with the actual implementation.

Do not document features that do not exist.

---

# 71. Deployment

The application should eventually be deployable to a Linux server/cloud environment.

Possible deployment:

```text
Spring Boot
      ↓
Docker
      ↓
Cloud/VPS
      ↓
MySQL
```

Production configuration must not contain development credentials.

---

# 72. Docker

If Docker is introduced:

Use separate configuration for:

```text
Application
MySQL
```

Do not bake secrets into Docker images.

Use environment variables.

Provide health checks where useful.

---

# 73. Important Rule About Existing Code

If a feature already works:

**Do not replace it simply because you prefer another coding style.**

First determine:

* What currently works?
* What is broken?
* What requirement changed?

Then modify only what is necessary.

---

# 74. Copilot Response Style

When asked to implement something, provide:

1. What needs to change
2. Files/classes affected
3. Implementation
4. Any database changes
5. How to test
6. Any configuration required

When code is requested, provide complete usable code rather than fragments that cannot compile.

If multiple files must change, clearly identify each file.

---

# 75. Do Not Guess Missing Requirements

If a business rule is genuinely unknown and changing it could affect financial/business data, ask for clarification.

For minor implementation details, choose a sensible default and document it.

Do not invent critical requirements such as:

* Tax percentage
* Laundry prices
* Business address
* UPI ID
* WhatsApp credentials
* Delivery capacity

These must be configurable or explicitly provided.

---

# 76. Current Priority

Implement the project in phases rather than attempting everything at once.

Recommended order:

## Phase 1 — Foundation

```text
Spring Boot setup
MySQL
Spring Security
Admin login
Base layout
Dashboard
```

## Phase 2 — Customers

```text
Customer CRUD
Search
Pagination
Customer history
```

## Phase 3 — Orders

```text
Order creation
Order items
Item pricing
Order numbers
Tag numbers
Status
```

## Phase 4 — Billing

```text
Payment
Balance
Invoice
PDF
QR payment
```

## Phase 5 — WhatsApp

```text
Meta WhatsApp API
Invoice sending
Message status
Retry
Error handling
```

## Phase 6 — Reports

```text
Daily
Weekly
Monthly
Item-wise
Payment reports
```

## Phase 7 — Production Hardening

```text
Validation
Security
Tests
Logging
Indexes
Performance
Docker
Deployment
```

---

# 77. Final Quality Standard

Before considering the project complete, it should satisfy this principle:

> This should be usable by an actual laundry business employee for daily operations.

It must not feel like:

* A college project
* A CRUD tutorial
* A static HTML demo
* A simple contact form
* A fake dashboard

The final application should have a coherent workflow:

```text
Customer
   ↓
Booking
   ↓
Order
   ↓
Item Tagging
   ↓
Processing
   ↓
Billing
   ↓
Payment
   ↓
Invoice
   ↓
WhatsApp
   ↓
Delivery
   ↓
Reports
```

Build the system incrementally, keep the architecture clean, protect business data, and prioritize correctness over unnecessary complexity.
