-- CertiTrust database structure (MySQL 8).
-- Reference script for the Phase-2 report. The application creates the same tables automatically
-- through Hibernate (spring.jpa.hibernate.ddl-auto=update), so running this file is optional.

CREATE DATABASE IF NOT EXISTS certitrust CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE certitrust;

CREATE TABLE institutions (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150) NOT NULL,
    address     VARCHAR(255),
    email       VARCHAR(150),
    website     VARCHAR(200),
    status      ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE',
    logo_path   VARCHAR(150),
    PRIMARY KEY (id),
    UNIQUE KEY uq_institutions_name (name)
);

CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password       VARCHAR(100) NOT NULL,              -- BCrypt hash, never plain text
    role           ENUM('ADMIN','ISSUER','VERIFIER') NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    institution_id BIGINT NULL,
    created_at     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_role (role),
    CONSTRAINT fk_users_institution FOREIGN KEY (institution_id) REFERENCES institutions (id)
);

CREATE TABLE certificates (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    certificate_id     VARCHAR(64)  NOT NULL,           -- public ID, e.g. CERT-2026-000001
    recipient_name     VARCHAR(120) NOT NULL,
    recipient_email    VARCHAR(150) NOT NULL,
    course             VARCHAR(200) NOT NULL,
    certificate_type   ENUM('COURSE_COMPLETION','DEGREE','DIPLOMA','PARTICIPATION','ACHIEVEMENT') NOT NULL,
    institution_id     BIGINT       NOT NULL,
    institution_name   VARCHAR(150) NOT NULL,           -- name at the time of issue (part of the hash)
    issuer_id          BIGINT       NOT NULL,
    issue_date         DATE         NOT NULL,
    expiry_date        DATE NULL,
    grade              VARCHAR(60),
    additional_info    VARCHAR(1000),
    status             ENUM('ACTIVE','REVOKED') NOT NULL DEFAULT 'ACTIVE',
    certificate_hash   VARCHAR(64)  NOT NULL,           -- unkeyed SHA-256 checksum; not a digital signature
    qr_code_path       VARCHAR(255),
    pdf_path           VARCHAR(255),
    verification_count BIGINT       NOT NULL DEFAULT 0,
    created_at         DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_certificates_certificate_id (certificate_id),
    KEY idx_cert_recipient (recipient_name),
    KEY idx_cert_status (status),
    KEY idx_cert_created (created_at),
    CONSTRAINT fk_cert_institution FOREIGN KEY (institution_id) REFERENCES institutions (id),
    CONSTRAINT fk_cert_issuer      FOREIGN KEY (issuer_id)      REFERENCES users (id)
);

CREATE TABLE revocations (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    certificate_id BIGINT       NOT NULL,
    revoked_by     BIGINT       NOT NULL,
    reason         VARCHAR(500) NOT NULL,
    revoked_at     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_rev_cert (certificate_id),
    CONSTRAINT fk_rev_certificate FOREIGN KEY (certificate_id) REFERENCES certificates (id),
    CONSTRAINT fk_rev_user        FOREIGN KEY (revoked_by)     REFERENCES users (id)
);

CREATE TABLE verification_logs (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    certificate_id      VARCHAR(64) NOT NULL,           -- text: unknown IDs are logged too
    verification_time   DATETIME    NOT NULL,
    verification_status ENUM('VERIFIED','REVOKED','EXPIRED','TAMPERED','NOT_FOUND') NOT NULL,
    verifier_id         BIGINT NULL,                    -- NULL = anonymous
    ip_address          VARCHAR(64),
    PRIMARY KEY (id),
    KEY idx_vlog_cert (certificate_id),
    KEY idx_vlog_time (verification_time),
    CONSTRAINT fk_vlog_user FOREIGN KEY (verifier_id) REFERENCES users (id)
);

CREATE TABLE audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    action      VARCHAR(50)  NOT NULL,
    actor       VARCHAR(150),
    details     VARCHAR(500),
    ip_address  VARCHAR(64),
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_time (created_at)
);
