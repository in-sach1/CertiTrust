# CertiTrust - Digital Certification Verification Platform

> **Verify Every Credential. Trust Every Certificate.**

CertiTrust is a full-stack Java web application. Institutions issue digital certificates (with a unique ID, a SHA-256
fingerprint, a QR code and a PDF), and anyone - recruiters, employers, universities - can verify them instantly,
with or without an account.

---

> **Version 1.1.0 - Phase 2 update.** Adds the object-oriented design from the Phase-2 report: account role hierarchy
> (inheritance / overriding), `CertificateOperations` and `VerificationOperations` interfaces, overloaded search and verify
> methods, custom exceptions for invalid IDs, a **Reports** page built with Java collections, search by type and date range,
> a public **/design** page, UML / ER / workflow diagrams (`docs/`) and unit tests. See `docs/PHASE2_IMPLEMENTATION.md`.

## Project Overview

| Who | What they can do |
|-----|------------------|
| **Anyone (no login)** | Verify a certificate by ID or QR scan, view a certificate page, download its PDF |
| **ISSUER** | Issue certificates, search / filter / sort / paginate them, download PDFs, revoke, see verification activity |
| **VERIFIER** | Verify certificates, search all certificates, keep a verification history (cannot issue or revoke) |
| **ADMIN** | Manage users, institutions and all certificates, view verification logs, audit trail and charts |

## Features

* Landing page with animated certificate/QR visual, "How it works", feature cards, security section and **live statistics from the database**
* Public **/verify** page: certificate ID input + **camera QR scanner** (and "scan from image")
* Result cards: green *Verified*, amber *Revoked* / *Expired*, red *Not found* / *Integrity check failed*
* **/certificate/{id}** details page with certificate preview, QR, hash, Download / Verify again / Print
* Issuing: unique ID `CERT-YYYY-NNNNNN`, SHA-256 hash, QR code PNG and professional A4 PDF - generated automatically
* Revocation with reason and **revocation history** (certificates are never deleted)
* Searchable, filterable, sortable, paginated certificate tables
* Admin: user CRUD (add / edit / enable / disable / delete / change role), institutions (with logo upload), verification logs, audit trail, 4 Chart.js charts
* Spring Security: BCrypt, roles, CSRF, sessions, friendly 403/404/500 pages
* REST API with consistent JSON responses and a global exception handler
* Audit trail for: certificate created / viewed / verified / revoked, login, logout, user changes
* **Phase 2:** Admin **Reports** (counts by type / institution / status, verification outcomes, most verified), search by certificate type and issue-date range, ID-format validation with helpful hints, public **/design** page with diagrams
* Responsive dark "digital trust" UI (glassmorphism, subtle animations) - desktop, tablet and mobile

## Technology Stack

Java 17 - Spring Boot 3.5 (MVC, Data JPA, Security, Validation) - Hibernate - MySQL 8 (H2 optional) - Thymeleaf with
Spring Security extras - Bootstrap 5 + Bootstrap Icons - Chart.js - html5-qrcode (browser QR scanner) -
**ZXing** (QR generation) - **OpenPDF** (PDF generation) - Maven.

## System Architecture

```
Browser (Thymeleaf pages + JS)   REST clients (Postman / curl)
              |                          |
        controller  (web MVC)     controller.api (JSON)
              \_______________  _________/
                              \/
                           service        <- business rules, hashing, QR, PDF, audit
                              |
                          repository      <- Spring Data JPA
                              |
                     MySQL (or H2)  +  storage/ folder (PDF, QR, logos)
```

**How verification works:** on issue, CertiTrust stores an unkeyed SHA-256 fingerprint of
`certificateId | recipient | course | institution | issueDate | type | expiry | grade` and recomputes it during
verification. This detects accidental corruption or a change to the protected fields when the stored fingerprint is
left unchanged. It is a checksum, **not a digital signature**: a database writer who can change both the certificate
record and its fingerprint can recompute the hash. It does not provide authenticity against a malicious database
administrator. See [the security and migration notes](docs/SECURITY_AND_MIGRATION.md) for the backward-compatible
signature design and production migration procedure. Legitimate edits (API `PUT`) re-seal the certificate with a new
fingerprint, QR and PDF.

## Database Setup

### Option A - MySQL (default)
1. Install and start MySQL 8 (macOS: `brew install mysql && brew services start mysql`).
2. Nothing else is required: the database `certitrust` is created automatically (`createDatabaseIfNotExist=true`)
   and Hibernate creates all tables on first start.
3. If your MySQL `root` user has a password, export it (see *Configuration*).

Tables: `users`, `institutions`, `certificates`, `verification_logs`, `revocations`, `audit_logs`
(primary keys, foreign keys, unique constraints on `users.email`, `institutions.name`, `certificates.certificate_id`,
and indexes on recipient, status, creation time, verification time and more).

### Option B - no MySQL needed (H2 file database)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
Data is stored in `./data/`. Everything else behaves the same.

## Installation

Requirements: **JDK 17+** (`java -version`) and **Maven 3.8+** (`mvn -v`; macOS: `brew install maven`).

```bash
cd CertiTrust
mvn clean install
```

## Configuration

`src/main/resources/application.yml` - every value can be overridden by an environment variable:

| Setting | Env variable | Default |
|---------|--------------|---------|
| Database URL | `DB_URL` | `jdbc:mysql://localhost:3306/certitrust?createDatabaseIfNotExist=true&...` |
| Database user | `DB_USERNAME` | `root` |
| Database password | `DB_PASSWORD` | *(empty)* |
| Hibernate schema mode | `DB_DDL_AUTO` | `update` |
| Server port | `PORT` | `8080` |
| Mark session cookie as Secure | `SESSION_COOKIE_SECURE` | `false` (enable behind HTTPS) |
| Public base URL (used inside QR codes and PDFs) | `APP_BASE_URL` | `http://localhost:8080` |
| Load demo data on an empty database | `APP_DEMO_DATA` | `false` |
| First administrator email (empty database only) | `INITIAL_ADMIN_EMAIL` | *(unset)* |
| First administrator password (empty database only) | `INITIAL_ADMIN_PASSWORD` | *(unset)* |
| Store client IPs in verification logs | `APP_LOG_IP` | `true` |
| File upload folder | `UPLOAD_DIR` | `./storage/uploads` |
| Certificate PDF folder | `CERTIFICATE_DIR` | `./storage/certificates` |
| QR code folder | `QR_DIR` | `./storage/qr` |

Example: `DB_PASSWORD=secret mvn spring-boot:run`

For a new real deployment, set `INITIAL_ADMIN_EMAIL` and `INITIAL_ADMIN_PASSWORD` together. The application creates
that administrator only when the database contains no users; the password is BCrypt-hashed and is never logged.
Use a secret manager or protected environment configuration for the password. Bootstrap credentials are ignored once
the database contains any users.

For production, use HTTPS and set `SESSION_COOKIE_SECURE=true`; configure `DB_URL` with
`sslMode=VERIFY_IDENTITY` and the database CA certificate; set `DB_DDL_AUTO=validate` after applying a reviewed schema
migration; and use a least-privilege database user. The default local MySQL URL uses preferred TLS to keep development
convenient, while production deployments should require certificate-verified TLS.

> To scan QR codes with a phone, set `APP_BASE_URL` to an address the phone can reach (e.g. your LAN IP or a real domain)
> **before** issuing certificates, because the URL is written into each QR code.

## Running the Application

### One-command local demo

From the project directory, start an isolated in-memory H2 demo with sample accounts and certificates:

```bash
./run-demo.sh
```

Open **http://localhost:8080/login** and use one of the demo credentials below. This launcher uses a temporary,
in-memory database instead of the persistent `./data/certitrust` database, enables demo data for this run only, and
ignores inherited initial-admin bootstrap variables. Stop it with **Ctrl+C**; the in-memory demo data is discarded.
It does not delete or change your existing H2 database. Java 17+ and Maven must be installed.
If port 8080 is already in use, run `PORT=8081 ./run-demo.sh` and open **http://localhost:8081/login** instead.

To run against the persistent H2 profile or configure a real administrator for a new database, use the manual commands
below instead.

```bash
export INITIAL_ADMIN_EMAIL=admin@example.com
export INITIAL_ADMIN_PASSWORD='replace-with-a-strong-unique-password'
mvn spring-boot:run                                   # MySQL
mvn spring-boot:run -Dspring-boot.run.profiles=h2     # H2, no MySQL needed
```
Open **http://localhost:8080**. (Or: `mvn clean install` then `java -jar target/certitrust-1.1.0.jar`.)

To use the predictable sample users and sample certificates locally, explicitly enable them:

```bash
APP_DEMO_DATA=true mvn spring-boot:run
```

## Demo Credentials (demo data only; never use in a real deployment)

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@demo.com` | `Admin@123` |
| Issuer | `issuer@demo.com` | `Issuer@123` |
| Verifier | `verifier@demo.com` | `Verifier@123` |
| Issuer (2nd institution) | `issuer2@demo.com` | `Issuer@123` |

The login page shows these with click-to-fill buttons only while `APP_DEMO_DATA=true`. Demo data = 3 institutions, 4 users,
8 certificates (one revoked, one expired) and sample verification logs spread over the last months so the charts have content.
Demo data is disabled by default.

Try these certificate IDs on **/verify** after the first start - the exact IDs are shown in the Issuer's certificate table
(they look like `CERT-2026-000001`).

### 5-minute walkthrough
1. Home page -> **Verify Certificate** -> enter an ID from the issuer's table -> green *Verified* card.
2. Log in as `issuer@demo.com` -> **Issue Certificate** -> fill the form -> you land on the certificate page with QR and **Download Certificate** (PDF).
3. **Certificates** -> search, filter *Valid / Revoked / Expired*, sort by clicking headers, **Revoke** one with a reason -> verify it again -> amber *Certificate Revoked* with date and reason.
4. Log in as `verifier@demo.com` -> verify, search, **Verification History**.
5. Log in as `admin@demo.com` -> charts, **Users**, **Institutions** (upload a logo - it appears on new PDFs), **Audit Trail**.

## API Documentation

All responses use one envelope: `{ "success": true|false, "message": "...", "data": ..., "timestamp": "..." }`.
Errors: `400` validation, `401` not authenticated, `403` forbidden, `404` not found, `500` unexpected.
`{certificateId}` is the public ID (e.g. `CERT-2026-000001`) - database ids are never exposed.

**Authentication:** either HTTP Basic (`curl -u issuer@demo.com:Issuer@123 ...`, stateless) or `POST /api/auth/login` (session cookie).
CSRF protection stays on for browser sessions; requests that carry an `Authorization` header and the `/api/auth/*` endpoints are exempt.

| Method & path | Access | Description |
|---------------|--------|-------------|
| `POST /api/auth/register` | public | Register a VERIFIER or ISSUER (issuers need admin approval) |
| `POST /api/auth/login` | public | Start a session |
| `GET /api/certificates/{certificateId}` | public | Public-safe certificate data (no e-mail) |
| `GET /api/certificates/{certificateId}/verify` | public | Verify (counted + logged, same as the website) |
| `GET /api/certificates?q=&filter=&page=&size=&sort=&dir=` | ISSUER (own) / ADMIN / VERIFIER (all) | Search, `filter` = ALL, VALID, REVOKED, EXPIRED |
| `POST /api/certificates` | ISSUER | Issue a certificate |
| `PUT /api/certificates/{certificateId}` | ISSUER (own) / ADMIN | Update an active certificate (re-seals hash, QR, PDF) |
| `POST /api/certificates/{certificateId}/revoke` | ISSUER (own) / ADMIN | Revoke with `{"reason": "..."}` |
| `GET /api/verification/history?page=&size=` | any logged-in role | Own checks (verifier) / checks on own certificates (issuer) / all (admin) |

Examples:
```bash
# public verification
curl http://localhost:8080/api/certificates/CERT-2026-000001/verify

# issue a certificate (HTTP Basic)
curl -u issuer@demo.com:Issuer@123 -X POST http://localhost:8080/api/certificates \
  -H 'Content-Type: application/json' \
  -d '{"recipientName":"Jane Doe","recipientEmail":"jane@example.com","course":"Intro to Security",
       "certificateType":"COURSE_COMPLETION","issueDate":"2026-10-01","expiryDate":"2029-10-01","grade":"A"}'

# revoke
curl -u issuer@demo.com:Issuer@123 -X POST http://localhost:8080/api/certificates/CERT-2026-000009/revoke \
  -H 'Content-Type: application/json' -d '{"reason":"Issued by mistake"}'
```
Optional list filters: `type` (see values below), `from` and `to` (issue dates, `yyyy-MM-dd`).
`certificateType` values: `COURSE_COMPLETION`, `DEGREE`, `DIPLOMA`, `PARTICIPATION`, `ACHIEVEMENT`.

## Project Structure

```
CertiTrust/
|-- pom.xml
|-- docs/         schema.sql, PHASE2_IMPLEMENTATION.md, diagrams/ (UML, ER, workflows as .svg/.png/.dot)
|-- src/main/java/com/digitalcertificate/
|   |-- DigitalCertificateApplication.java
|   |-- domain/       Account + Administrator/Issuer/Verifier accounts, Permission (pure Java)
|   |-- config/       AppProperties, SecurityConfig, DemoDataLoader
|   |-- controller/   web MVC controllers (+ api/ for REST)
|   |-- dto/          API records, forms, VerificationResult
|   |-- entity/       User, Institution, Certificate, VerificationLog, Revocation, AuditLog + enums
|   |-- repository/   Spring Data repositories, CertificateSpecs (dynamic search)
|   |-- security/     AppUserDetails(+Service), JsonAuthEntryPoint, LoginSuccessHandler
|   |-- service/      Certificate/User/Institution/Dashboard/Audit/Qr/Pdf/Storage services
|   |-- exception/    BusinessException, ResourceNotFoundException, ApiExceptionHandler
|   `-- util/         HashUtil, Patterns, RequestInfo
|-- src/test/java/ unit tests (JUnit 5)
`-- src/main/resources/
    |-- application.yml, application-h2.yml
    |-- static/       css/style.css, js/app.js, js/scanner.js, img/logo.svg
    `-- templates/    fragments/, index, verify, certificate/, issuer/, verifier/, admin/, error/ ...
```

## Screenshots

Screenshots are not bundled. After starting the app, these pages make good captures:
Home (`/`), Verify result (`/verify?certificateId=...`), Certificate page (`/certificate/...`),
Issuer dashboard, Issue form, Admin dashboard (charts), Users, Institutions, a downloaded PDF.

## Security Notes

* Passwords: BCrypt; at least 8 characters with a letter and number, limited to 72 UTF-8 bytes to avoid BCrypt truncation. CSRF tokens on every form. Thymeleaf escapes all output (XSS). SQL via JPA only.
* Roles enforced twice: URL rules in `SecurityConfig` and `@PreAuthorize` / ownership checks in services.
* Uploaded logos: content-sniffed (PNG/JPEG only), max 1 MB, dimensions checked before decoding (maximum 4000 x 4000), stored under a generated name.
* Institution website links must start with `http://` or `https://`.
* Public pages never show recipient e-mail or internal ids. Session cookies are `HttpOnly`, `SameSite=Lax`.
* Verification logs store IP addresses (`APP_LOG_IP=false` turns this off) - inform users / check local privacy law.
* Demo accounts are disabled by default; `APP_DEMO_DATA=true` is for isolated local demonstrations only.
* Certificate fingerprints are unkeyed checksums, not signatures. They cannot detect an attacker who can update both a certificate row and its hash. Review the [security and migration notes](docs/SECURITY_AND_MIGRATION.md) before production use or schema changes.

## Differences from the original specification (closest working alternatives)

| Spec item | What is implemented | Why |
|-----------|---------------------|-----|
| Registration with role ADMIN | Self-registration offers **ISSUER** and **VERIFIER** only; admins are created by admins | Anyone could otherwise make themselves an administrator |
| Issuer registration | New issuer accounts are **disabled until an admin enables them**; they also create their institution | Prevents anybody from issuing certificates in an institution's name |
| Hash inputs | Spec fields **plus** type, expiry and grade; institution *name at issue time* is stored on the certificate | Stronger integrity; renaming an institution later cannot invalidate old certificates |
| Contact form | Message is saved to the audit log (visible to admins) | No mail server is configured |
| Notifications bell | Shows the latest activity (verifications / audit events) | No separate notification system was specified |
| MySQL only | MySQL default **plus** an `h2` profile | Lets you run the project without installing MySQL |
| API `PUT /api/certificates/{id}` | `{id}` is the public certificate ID | Internal database ids are not exposed |
| Fonts / libraries | Bootstrap, Chart.js, html5-qrcode and Google Fonts load from CDNs (internet needed) | Keeps the repository small |
| PDF text | Standard PDF fonts (Latin characters) | Non-Latin recipient names may not render in the PDF |
| Automated tests | Unit tests for hashing, ID validation and the role classes (`mvn test`) | Run without a database; end-to-end tests are future work |

## Future Enhancements

Institution approval workflow with e-mail, e-mail delivery of certificates, bulk issuing via CSV, digital signatures with
asymmetric keys (RSA/ECDSA), blockchain anchoring of hashes, rate limiting on verification, multi-language PDFs,
JUnit/Selenium tests, Docker Compose (app + MySQL), and OpenAPI/Swagger documentation.

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Communications link failure` / app stops at start-up | MySQL is not running. Start it, or use the H2 profile |
| `Access denied for user 'root'` | `export DB_USERNAME=... DB_PASSWORD=...` before running |
| `release version 17 not supported` / class version errors | You are on JDK < 17. Install JDK 17+ and check `java -version` and `mvn -v` |
| `Port 8080 was already in use` | `PORT=9090 mvn spring-boot:run` (also set `APP_BASE_URL`) |
| `mvn: command not found` | macOS: `brew install maven`; Windows: install Maven and add it to PATH |
| A dependency cannot be resolved | Check your internet connection / proxy; run `mvn -U clean install` |
| Pages look unstyled / charts missing | The browser could not reach the CDNs (Bootstrap, Chart.js) - check internet access |
| Camera scanner says "camera unavailable" | Browsers allow the camera only on `https://` or `localhost`. Use *Scan from image* otherwise |
| QR code opens the wrong address | Set `APP_BASE_URL` to your real address **before** issuing; old QR codes keep the old URL |
| Issuer cannot log in after registering | An admin must enable the account (Admin -> Users) |
| Want a fresh start | MySQL: `DROP DATABASE certitrust;` - H2: delete `./data`. Also delete `./storage` to remove PDFs/QRs |
