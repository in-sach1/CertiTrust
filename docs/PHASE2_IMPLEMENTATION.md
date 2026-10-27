# Phase 2 report -> implementation

This file shows where each item of the Phase-2 report (*Design and Core Development Planning*) is implemented in
CertiTrust, how it is tested, and where the evidence (diagrams, workflows) lives.

## 1. Report items -> code

| Phase-2 report item | Implementation |
|---|---|
| Certificate attributes: ID, holder name, course/event title, issuing organisation, issue date, verification status | `entity/Certificate` (`certificateId`, `recipientName`, `course`, `institutionName`, `issueDate`, `status`, `getDisplayStatus()`) |
| User roles: administrator, issuer, verifier | `entity/Role`; role classes `domain/AdministratorAccount`, `IssuerAccount`, `VerifierAccount` |
| **Inheritance** - reuse common properties across roles | `domain/Account` (abstract) is extended by the three role classes; `InvalidInputException extends BusinessException`; `CertificateNotFoundException extends ResourceNotFoundException` |
| **Polymorphism - overriding** (role-specific behaviour) | `role()`, `permissions()`, `dashboardPath()`, `describe()` overridden per role. Used at run time: `DashboardController` (login redirect) and `HistoryController` (who may see IP addresses) call them on an `Account` |
| **Polymorphism - overloading** (flexible search and verification) | `Account.can(Permission)` / `can(String)`; `CertificateService.search(...)` x5 (criteria, classic, keyword, type, date range); `verify(id)` / `verify(id, verifierId, ip)`; `CertificateSpecs.build(...)` x2 |
| **Abstraction and interfaces** | abstract `Account`; interfaces `service/CertificateOperations` and `service/VerificationOperations`, both implemented by `CertificateService` |
| **Encapsulation** | private fields + accessors in all entities and in `Account`; BCrypt-hashed passwords |
| **Collections** - store and manage many records dynamically | `service/ReportService` (List, Set/TreeSet, EnumMap, TreeMap, LinkedHashMap, Comparator) shown at **Admin -> Reports**; `EnumSet` permission sets |
| **Exception handling** - invalid IDs, missing records, incorrect input | `InvalidCertificateIdException` (format `CERT-YYYY-NNNNNN`), `CertificateNotFoundException`, `InvalidInputException`; `ApiExceptionHandler` (JSON) and friendly 400/403/404/500 pages |
| Modules: user management, certificate management, issuance, search, verification, status tracking | see the module table on the **/design** page |
| Search by attributes | status, certificate type, issue-date range, keyword (ID / recipient / course / institution) on every certificate table and `GET /api/certificates?type=&from=&to=` |
| Verification status for each certificate | `VERIFIED / REVOKED / EXPIRED / TAMPERED / NOT_FOUND` + `VerificationLog` history |
| Validation and exception handling | Bean Validation on every form, `CertificateIdValidator`, service-level rules |
| Testing with sample records | demo data (`DemoDataLoader`) + unit tests in `src/test/java` (see below) |

## 2. Evidence for the report

| Evidence named in the report | File |
|---|---|
| UML / class diagram | `docs/diagrams/class-roles.svg/.png`, `class-services.svg/.png`, `entity-diagram.svg/.png` (sources: `.dot`) |
| Database structure | `docs/diagrams/er-diagram.svg/.png` and `docs/schema.sql` |
| Workflow (issuing) | `docs/diagrams/issue-flow.svg/.png` |
| Verification flow | `docs/diagrams/verify-flow.svg/.png` |
| Module structure, Java concept notes | **http://localhost:8080/design** (public page) and the tables in this file |

The SVG files scale without losing quality; the PNG files can be pasted straight into the Word report.
To change a diagram edit the `.dot` file and run `dot -Tsvg file.dot -o file.svg` (Graphviz).

## 3. Sample data used for testing

`DemoDataLoader` creates (on an empty database) 3 institutions, 4 users (admin, two issuers, one verifier) and 8 certificates:
valid, one revoked, one expired, plus verification logs including unknown IDs. Try these checks on **/verify**:

| Input | Expected result |
|---|---|
| an ID of a valid demo certificate (see the issuer's table) | green **Verified** |
| the demo revoked certificate | amber **Revoked** with date and reason |
| the demo expired certificate | amber **Expired** |
| `CERT-2026-999999` | red **Not found** |
| `hello` | red **Not found** + hint that the ID format is `CERT-YYYY-NNNNNN` |
| a valid ID after **editing the title in the database** | red **Integrity check failed** (tamper detection) |

The final example changes a protected field without changing its stored fingerprint. The fingerprint is an unkeyed
SHA-256 checksum, not a signature: a database writer who can change both values can recompute it. See
[`SECURITY_AND_MIGRATION.md`](SECURITY_AND_MIGRATION.md) for the threat boundary and a backward-compatible signature
design proposal.

## 4. Automated tests (`mvn test`)

| Test class | Checks |
|---|---|
| `HashUtilTest` | SHA-256 known test vector, length, sensitivity to changes |
| `CertificateHasherTest` | same data -> same hash; editing name / course / grade / expiry changes the hash |
| `CertificateIdValidatorTest` | accepts `CERT-YYYY-NNNNNN` (any case), rejects malformed and injection-like input |
| `AccountHierarchyTest` | factory creates the right subclass; overridden dashboards and permissions; overloaded `can(...)`; polymorphic `describe()` |

## 5. Not covered here

Screenshots of the running application and the written project report itself (results, conclusion) belong to the
final phase - take the screenshots from your own running copy.
