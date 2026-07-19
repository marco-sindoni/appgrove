# Record of processing activities (RoPA) — appgrove

> **INTERNAL** document (art. 30(4) GDPR): provided to the supervisory authority on request only; this is not the public privacy policy (#13 C17). **GENERATED file** from the data manifests (`docs/compliance/manifests/*.yaml`) via `tools/compliance` — **do not edit by hand**: update the manifest and regenerate (`npm run assemble`). Draft under disclaimer: final validation by legal counsel (docs/_REVISIONE-LEGALE.md).

## appgrove platform (core)

Cross-cutting platform processing (accounts, users, invitations, authentication, logs). appgrove acts as controller towards B2C consumers and as processor for B2B tenant controllers (#13 C13). Paddle (Merchant of Record) is an independent controller for payment data.

### Processing activities

| Entry | Data category | Location | Data subjects | Purpose | Legal basis | Retention |
|---|---|---|---|---|---|---|
| `users.email` | Contact data (email address) | Table `platform.users` (Aurora PostgreSQL, eu-west-1) | Registered users (members of an account/tenant) | Account provisioning and management (login, service communications) | Contract (art. 6(1)(b) GDPR) | While the account is active + 14-day grace period after deletion (#13 E25) |
| `users.display_name` | Identity (display name) | Table `platform.users` (Aurora PostgreSQL, eu-west-1) | Registered users | User identification in the UI and within tenant collaboration | Contract (art. 6(1)(b) GDPR) | While the account is active + 14-day grace period after deletion (#13 E25) |
| `users.locale` | User preference (language) | Table `platform.users` (Aurora PostgreSQL, eu-west-1) | Registered users | Language selection (English/Italian) for transactional authentication emails (address verification, password reset, invitation) — UC 0018 | Contract (art. 6(1)(b) GDPR) — transactional emails, distinct from the newsletter (separate consent, | While the account is active + 14-day grace period after deletion (#13 E25) |
| `users.cognito_sub` | Online identifier (Cognito subject, pseudo-identifier) | Table `platform.users` (Aurora PostgreSQL, eu-west-1) | Registered users | Link between the authentication identity (Cognito) and the application profile | Contract (art. 6(1)(b) GDPR) | While the account is active + 14-day grace period after deletion (#13 E25) |
| `invitations.email` | Contact data (invitee email address) | Table `platform.invitations` (Aurora PostgreSQL, eu-west-1) | People invited to join a tenant (not yet users) | Delivery and management of the invitation (single-use, expiring; the token is stored hashed only) | Pre-contractual measures / contract (art. 6(1)(b) GDPR) | Until the invitation expires or is accepted; soft-delete and purge per the platform lifecycle |
| `accounts.name` | Identity / account name (prudent classification — may be a company name) | Table `platform.accounts` (Aurora PostgreSQL, eu-west-1) | Account holders (for individual B2C accounts it is typically the person's name) | Identification of the account/tenant across the platform and communications | Contract (art. 6(1)(b) GDPR) | While the account is active + 14-day grace period after deletion (#13 E25) |
| `accounts.paddle_customer_id` | Online identifier (customer id at Paddle, pseudo-identifier) | Table `platform.accounts` (Aurora PostgreSQL, eu-west-1) | Account holders with a subscription | Subscription/payment reconciliation with Paddle (Merchant of Record, independent controller — #13 H) | Contract (art. 6(1)(b) GDPR) | While the account is active + 14-day grace period; fiscal retention on payments rests with Paddle |
| `cognito.credentials` | Authentication credentials (password hash) and MFA/TOTP secrets | Amazon Cognito user pool (eu-west-1); locally a dev-only provider (`services/auth`, out of RoPA scope) | Registered users | User authentication and access protection (MFA) | Contract (art. 6(1)(b) GDPR) | While the account is active; removed when the Cognito user is deleted |
| `support_ticket.subject` | Free-form content (subject of the support/privacy request) | Table `platform.support_ticket` (Aurora PostgreSQL, eu-west-1) | Users opening support or data-rights requests | Handling support and data-rights requests (in-house ticketing, #13 D21) | Contract (art. 6(1)(b) GDPR); for privacy tickets also legal obligation (art. 12 GDPR) | 24 months from ticket closure, then automatic deletion (#13 E) |
| `support_ticket_message.body` | Free-form content (support-thread message text) | Table `platform.support_ticket_message` (Aurora PostgreSQL, eu-west-1) | Users writing in a support-ticket thread | Handling support and data-rights requests (in-house ticketing, #13 D21) | Contract (art. 6(1)(b) GDPR); for privacy tickets also legal obligation (art. 12 GDPR) | 24 months from ticket closure, then automatic deletion (#13 E) |
| `logs.structured` | Technical identifiers in structured logs (`tenant_id`, `app_id`, `user_id`, IP) | CloudWatch Logs (eu-west-1/eu-central-1); audit trail on S3/Glacier (#08 I) | Platform users | Security, stability and diagnostics (purpose limitation — no profiling, #13 B12) | Legitimate interest (art. 6(1)(f) GDPR — service security and continuity) | Prod application logs 30 days; audit/security 12 months on S3→Glacier (#08 I26) |
| `logs.frontend_errors` | JavaScript ERROR-only events (#08 H23): message/stack, route, build version, opaque `user_id`/`tenant_id` when a session exists. No IP, no user agent, no behavioural tracking. | CloudWatch Logs (eu-west-1), error-ingest log group (`/aws/lambda/appgrove-<env>-error-ingest`) | SPA users (including unauthenticated) | Frontend diagnostics and stability (purpose limitation — errors only, #13 B12) | Legitimate interest (art. 6(1)(f) GDPR — service quality and continuity) | Same as application logs (test 7 days, prod 30 days, |

### Recipients and sub-processors

Sub-processors: **AWS** (hosting, EU regions — DPA with SCCs + DPF certification) and **Plausible Analytics** (cookieless analytics, EU hosting). **Paddle** (Merchant of Record) is an **independent controller** for payment data, not a sub-processor (#13 H45-47). Public list: `content/subprocessors.md` (UC 0002).

### Non-EU transfers

Data at rest only in EU regions (eu-west-1; monitoring in eu-central-1) — #13 I51. AWS Inc. (US, CLOUD Act): DPF + SCC safeguards in the DPA, plus at-rest/in-transit encryption (#13 I52).

### Security measures

At-rest and in-transit encryption; row-level per-tenant isolation (`tenant_id` only from the verified JWT); least-privilege IAM; invitation tokens stored hashed only; soft-delete with scheduled purge (14-day grace); structured logging and audit trail (#02/#05/#06/#08).

## Fatture app (single-user B2C invoicing)

End-customer data entered by the tenant in their invoices (schema `app_fatture`). The tenant is the data controller; appgrove acts as processor (#13 C13).

### Processing activities

| Entry | Data category | Location | Data subjects | Purpose | Legal basis | Retention |
|---|---|---|---|---|---|---|
| `invoice.customer_name` | Customer identity (name) | Table `app_fatture.invoice` (Aurora PostgreSQL, eu-west-1) | Customers (invoice recipients) of the tenant | Invoice issuance and management | Contract (art. 6(1)(b) GDPR) and the controller's legal obligations (art. 6(1)(c)) | 10 years from issuance (fiscal obligations) |
| `invoice.customer_email` | Customer contact data (email address, optional) | Table `app_fatture.invoice` (Aurora PostgreSQL, eu-west-1) | Customers (invoice recipients) of the tenant | Invoice delivery | Contract (art. 6(1)(b) GDPR) | 10 years from issuance (fiscal obligations) |

