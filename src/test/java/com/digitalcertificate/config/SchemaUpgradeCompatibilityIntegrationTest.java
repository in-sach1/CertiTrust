package com.digitalcertificate.config;

import com.digitalcertificate.DigitalCertificateApplication;
import com.digitalcertificate.repository.CertificateRepository;
import com.digitalcertificate.service.CertificateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaUpgradeCompatibilityIntegrationTest {
    private static final String CERTIFICATE_ID = "CERT-2025-000042";
    private static final String LEGACY_HASH =
            "6cd4b3f29f8800b5da7984347adbe27edb685190a1d66fc3aaadd4f90d116a74";

    @Test
    void hibernateSchemaUpdatePreservesExistingCertificateFingerprint(@TempDir Path tempDirectory) throws Exception {
        String databaseUrl = "jdbc:h2:file:" + tempDirectory.resolve("existing-certitrust").toAbsolutePath()
                + ";MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE";
        createLegacySchemaAndData(databaseUrl);

        try (ConfigurableApplicationContext migrated = new SpringApplicationBuilder(DigitalCertificateApplication.class)
                .web(WebApplicationType.SERVLET)
                .run(
                        "--spring.datasource.url=" + databaseUrl,
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.jpa.hibernate.ddl-auto=update",
                        "--spring.main.banner-mode=off",
                        "--logging.level.org.apache.tomcat.util.net.Acceptor=OFF",
                        "--app.demo-data=false",
                        "--app.storage.upload-dir=" + tempDirectory.resolve("uploads"),
                        "--app.storage.certificate-dir=" + tempDirectory.resolve("certificates"),
                        "--app.storage.qr-dir=" + tempDirectory.resolve("qr"),
                        "--server.port=0")) {
            var certificate = migrated.getBean(CertificateRepository.class)
                    .findByCertificateId(CERTIFICATE_ID).orElseThrow();

            assertEquals(LEGACY_HASH, certificate.getCertificateHash());
            assertTrue(migrated.getBean(CertificateService.class).integrityOk(certificate));
        }
    }

    private static void createLegacySchemaAndData(String databaseUrl) throws Exception {
        try (var connection = DriverManager.getConnection(databaseUrl, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE institutions (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(150) NOT NULL UNIQUE,
                        address VARCHAR(255),
                        email VARCHAR(150),
                        website VARCHAR(200),
                        status VARCHAR(20) NOT NULL,
                        logo_path VARCHAR(150)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE users (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        email VARCHAR(150) NOT NULL UNIQUE,
                        password VARCHAR(100) NOT NULL,
                        role VARCHAR(20) NOT NULL,
                        enabled BOOLEAN NOT NULL,
                        institution_id BIGINT,
                        created_at TIMESTAMP NOT NULL,
                        CONSTRAINT fk_legacy_user_institution
                            FOREIGN KEY (institution_id) REFERENCES institutions (id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE certificates (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        certificate_id VARCHAR(64) NOT NULL UNIQUE,
                        recipient_name VARCHAR(120) NOT NULL,
                        recipient_email VARCHAR(150) NOT NULL,
                        course VARCHAR(200) NOT NULL,
                        certificate_type VARCHAR(30) NOT NULL,
                        institution_id BIGINT NOT NULL,
                        institution_name VARCHAR(150) NOT NULL,
                        issuer_id BIGINT NOT NULL,
                        issue_date DATE NOT NULL,
                        expiry_date DATE,
                        grade VARCHAR(60),
                        additional_info VARCHAR(1000),
                        status VARCHAR(20) NOT NULL,
                        certificate_hash VARCHAR(64) NOT NULL,
                        qr_code_path VARCHAR(255),
                        pdf_path VARCHAR(255),
                        verification_count BIGINT NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        CONSTRAINT fk_legacy_certificate_institution
                            FOREIGN KEY (institution_id) REFERENCES institutions (id),
                        CONSTRAINT fk_legacy_certificate_issuer
                            FOREIGN KEY (issuer_id) REFERENCES users (id)
                    )
                    """);
            statement.execute("""
                    INSERT INTO institutions (id, name, status)
                    VALUES (42, 'Legacy Institution', 'ACTIVE')
                    """);
            statement.execute("""
                    INSERT INTO users (id, name, email, password, role, enabled, institution_id, created_at)
                    VALUES (42, 'Legacy Issuer', 'legacy-issuer@example.test', 'not-used', 'ISSUER', TRUE, 42,
                            TIMESTAMP '2025-05-20 10:00:00')
                    """);
            statement.execute("""
                    INSERT INTO certificates (
                        id, certificate_id, recipient_name, recipient_email, course, certificate_type,
                        institution_id, institution_name, issuer_id, issue_date, expiry_date, grade,
                        additional_info, status, certificate_hash, verification_count, created_at
                    ) VALUES (
                        42, 'CERT-2025-000042', 'Legacy Recipient', 'legacy-recipient@example.test',
                        'Legacy course', 'COURSE_COMPLETION', 42, 'Legacy Institution', 42,
                        DATE '2025-05-20', NULL, 'Pass', NULL, 'ACTIVE',
                        '6cd4b3f29f8800b5da7984347adbe27edb685190a1d66fc3aaadd4f90d116a74',
                        7, TIMESTAMP '2025-05-20 10:00:00'
                    )
                    """);
        }
    }
}
