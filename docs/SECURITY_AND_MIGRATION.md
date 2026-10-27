# Certificate Integrity and Database Migration

This document describes the existing integrity check, a safe path to stronger integrity, and the precautions for
deploying schema changes. No production database or user data was accessed to prepare it.

## Current certificate fingerprint

`CertificateHasher` hashes the UTF-8 encoding of the pipe-delimited certificate ID, recipient name, course,
institution name, issue date, certificate type, expiry date, and grade using SHA-256. The value is stored in
`certificates.certificate_hash` and is recomputed during verification.

This is an **unkeyed checksum/fingerprint**, not a cryptographic signature. It detects accidental corruption or a
change to a protected field when the stored hash is not also changed. A database administrator or other writer who can
change both the certificate row and `certificate_hash` can calculate a matching hash. The current mechanism therefore
does not establish who issued a certificate or protect against a malicious database writer.

The current hash also excludes recipient email, `additional_info`, status, and the separate revocation record. Status
and revocation are checked independently by the application. The pipe-delimited input is a legacy format and is not a
canonical encoding for a new cryptographic protocol; a new version must use an unambiguous, versioned serialization
(for example, canonical JSON or a length-prefixed field encoding).

## Backward-compatible cryptographic design

Do not replace or reinterpret existing hashes in place. The regression vector in
`CertificateHasherTest.preservesPreviouslyIssuedCertificateFingerprint` and the synthetic schema-upgrade test protect
legacy verification behavior. A stronger scheme requires product decisions and production key-management
configuration before code or schema changes are made.

### Options

| Scheme | Protection and verification | Operational trade-off |
|---|---|---|
| HMAC-SHA-256 | A database-only writer without the HMAC key cannot recalculate the authenticator. The application can verify it, but a public verifier cannot verify it independently without access to the secret. | Store keys outside the database in a secrets manager/KMS. Keep old key IDs available for verification during rotation. A compromised application or key can still forge values. |
| Digital signature (for example, Ed25519) | A private signing key signs a versioned canonical certificate payload; the application and independent verifiers can validate it using a public key. A database-only writer cannot forge a signature. | Keep the private key in a KMS/HSM or equivalent protected signing service. Publish and retain public keys by key ID. Define issuer identity, signing authority, revocation, and recovery policy. |

### Required staged rollout

1. Decide whether verifiers need independent/offline verification (favoring signatures) or only application-mediated
   verification (HMAC may be sufficient). Select the approved algorithm, signing authority, certificate fields, and
   policy for revoked/expired certificates.
2. Provision key material outside the repository and database. For signatures, generate/use the private key in a
   KMS/HSM where possible and distribute public keys through an authenticated, documented channel. Never commit keys
   or place private key material in ordinary application configuration.
3. Design a versioned canonical payload and add a scheme/version and key ID. Store a signature/MAC in separate,
   nullable fields so the existing `certificate_hash` remains unchanged. New writes use the selected scheme; existing
   rows remain explicitly identified as legacy SHA-256 and continue to verify using the existing code path.
4. Verify new records by scheme and key ID. Reject unknown schemes, unknown key IDs, malformed authenticators, and
   invalid signatures/MACs; do not silently fall back to the legacy hash for a record marked as a newer scheme.
   Compare MACs in constant time.
5. Rotate by signing new records with a new key ID while retaining old verification keys for existing records.
   Retire an old key only after its records are expired/retired or an approved re-signing process has completed.
   For a suspected key compromise, follow an explicit incident/revocation policy; do not silently re-sign historical
   certificates and imply the original issuer authenticated them.
6. Preserve legacy rows and hashes. Do not claim that adding signatures later retroactively proves who issued an old
   certificate. Any migration of legacy records must require an independently trusted source/approval and leave an
   auditable record of the transition.
7. Add regression tests for fixed legacy hashes, dual-version verification, payload field changes, tampered signatures,
   unknown key IDs/schemes, key rotation, revocation, and database migration before enabling the new scheme.

No signing or HMAC implementation is included: the signing authority, verifier interoperability, key service, key
retention, and incident policy have not been selected.

## Database schema and test coverage

The application currently has no Flyway/Liquibase migrations or versioned incremental SQL scripts. The entity
mappings are the runtime schema model, `docs/schema.sql` is a reference initial MySQL schema, and
`spring.jpa.hibernate.ddl-auto` defaults to `update`. Automatic schema update is not a reviewed production migration
process. For production, use an explicitly reviewed/versioned MySQL migration and set `DB_DDL_AUTO=validate` after the
schema has been reconciled.

`SchemaUpgradeCompatibilityIntegrationTest` creates a **synthetic H2** legacy schema and record, starts Hibernate with
`ddl-auto=update`, then checks that the fixed legacy fingerprint is unchanged and the record still passes application
integrity verification. It does not connect to MySQL, verify the exact production schema, or test a production
database, its volume, constraints, or data distribution. No production data was read or modified. A production
migration needs a rehearsal against a sanitized, access-controlled copy of the exact source schema and representative
data.

## Existing MySQL database: backup, migration, and rollback

These are operator procedures for a planned release, not commands that were run against production. Confirm database
name, host, user, maintenance window, storage, and recovery objectives with the DBA first. Never point the following
commands at production until the backup and restore rehearsal is approved.

### 1. Prepare credentials and a private backup location

Use MySQL's local login-path store so commands do not contain a password. It prompts for the database password:

```bash
mysql_config_editor set --login-path=certitrust --host=YOUR_DB_HOST --user=YOUR_DB_USER --password
mkdir -p "$HOME/certitrust-backups"
chmod 700 "$HOME/certitrust-backups"
umask 077
BACKUP_FILE="$HOME/certitrust-backups/certitrust-before-$(date +%Y%m%d-%H%M%S).sql"
```

Keep the login-path configuration and backups outside the project directory. Do not commit either.

### 2. Back up and verify the dump

Run this before applying DDL or deploying a version that expects schema changes:

```bash
mysqldump --login-path=certitrust --single-transaction --routines --triggers --events --hex-blob certitrust > "$BACKUP_FILE"
test -s "$BACKUP_FILE"
shasum -a 256 "$BACKUP_FILE"
```

`--single-transaction` provides a consistent snapshot for transactional tables (normally InnoDB); coordinate a
maintenance window and use the database's approved backup procedure if any relevant tables are non-transactional.
Record the dump checksum outside the project. Restore the dump to an isolated MySQL instance and compare expected
table/row counts before approving the production change.

### 3. Apply a reviewed migration

For a release with no schema changes, do not run the reference `docs/schema.sql` against an existing database; deploy
with the reviewed production setting `DB_DDL_AUTO=validate`.

For a schema change, first author and review a versioned, MySQL-8-compatible incremental migration, rehearse it against
the restored copy, and verify constraints, row counts, and representative certificate fingerprints. During the
approved maintenance window, stop application writers, take the final backup above, then apply the exact reviewed
script:

```bash
mysql --login-path=certitrust certitrust < db/migration/V001__describe_change.sql
```

The path above is an example location only; this project does not currently contain such migration scripts. Do not
enable `DB_DDL_AUTO=update` as a substitute for reviewing a production schema change. Deploy the application with
`DB_DDL_AUTO=validate`, run the approved smoke checks, and monitor startup and database errors before reopening writes.

### 4. Roll back without overwriting the original database

If validation fails, stop application writers and restore the pre-migration backup into a **new, empty recovery
database**. This avoids deleting or overwriting the original database while recovery is verified:

```bash
RESTORE_DB="certitrust_restore_$(date +%Y%m%d_%H%M%S)"
mysql --login-path=certitrust -e "CREATE DATABASE \`$RESTORE_DB\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
mysql --login-path=certitrust "$RESTORE_DB" < "$BACKUP_FILE"
```

Verify table and row counts, foreign keys, and sample certificate verification against the restored database. Then
have the DBA approve repointing the application to that recovery database and start it with
`DB_DDL_AUTO=validate`. Keep the failed database read-only until the incident is understood. Restoring over the
original database can destroy transactions written after the backup; it requires explicit DBA/data-owner approval and
a separate recovery plan.

## Browser and responsive verification

No Playwright, Puppeteer, Selenium, or browser driver is currently configured in this repository/environment, and no
browser workflow or viewport test is claimed as passed. To add browser testing in a development environment:

1. Install Node.js/npm, then from the project root add Playwright as a development-only dependency and install
   Chromium:

   ```bash
   npm install --save-dev @playwright/test
   npx playwright install chromium
   ```

2. In one terminal, start CertiTrust on an isolated in-memory H2 database. Demo data is enabled only for this local
   instance; the upload and generated-certificate folders are outside the repository:

   ```bash
   SPRING_DATASOURCE_URL='jdbc:h2:mem:certitrust-browser;MODE=MySQL;DB_CLOSE_DELAY=-1' \
   SPRING_DATASOURCE_USERNAME=sa \
   SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver \
   APP_DEMO_DATA=true \
   APP_BASE_URL=http://localhost:8080 \
   UPLOAD_DIR=/tmp/certitrust-browser/uploads \
   CERTIFICATE_DIR=/tmp/certitrust-browser/certificates \
   QR_DIR=/tmp/certitrust-browser/qr \
   PORT=8080 mvn spring-boot:run
   ```

   Use only the documented demo accounts while `APP_DEMO_DATA=true`; never use production credentials or data.
3. Add browser tests for homepage/navigation, login/registration, certificate issue/PDF/QR verification,
   revocation, administrator pages/uploads, validation/error cases, and responsive viewport widths such as 390,
   768, and 1440 pixels. Verify public/role-restricted routes and capture console/network errors.

   Run the suite in another terminal with:

   ```bash
   npx playwright test
   ```

4. Run the tests in a real browser and retain the test output/artifacts. The current Java integration tests cover API
   security and service workflows, but do not replace browser-level interaction or responsive layout verification.
