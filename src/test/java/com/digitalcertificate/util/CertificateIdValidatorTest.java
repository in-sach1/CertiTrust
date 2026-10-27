package com.digitalcertificate.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CertificateIdValidatorTest {

    @Test
    void acceptsWellFormedIds() {
        assertTrue(CertificateIdValidator.isValid("CERT-2026-000001"));
        assertTrue(CertificateIdValidator.isValid("CERT-2026-123456789"));
    }

    @Test
    void ignoresCaseAndSurroundingSpaces() {
        assertTrue(CertificateIdValidator.isValid("  cert-2026-000123 "));
    }

    @Test
    void rejectsMalformedIds() {
        assertFalse(CertificateIdValidator.isValid(null));
        assertFalse(CertificateIdValidator.isValid(""));
        assertFalse(CertificateIdValidator.isValid("CERT-26-1"));
        assertFalse(CertificateIdValidator.isValid("CERT-2026-12"));
        assertFalse(CertificateIdValidator.isValid("1; DROP TABLE certificates"));
    }
}
