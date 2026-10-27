package com.digitalcertificate.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Sample certificate data: the hash must change if any protected field is edited (tamper detection). */
class CertificateHasherTest {
    private static final LocalDate ISSUED = LocalDate.of(2026, 5, 20);

    private String hash(String name, String course, String grade) {
        return CertificateHasher.compute("CERT-2026-000001", name, course, "Demo Institute of Technology",
                ISSUED, "COURSE_COMPLETION", null, grade);
    }

    @Test
    void sameDataGivesSameHash() {
        assertEquals(hash("Aarav Sharma", "Java", "A"), hash("Aarav Sharma", "Java", "A"));
    }

    @Test
    void preservesPreviouslyIssuedCertificateFingerprint() {
        assertEquals("ceeb927627e7b48c8d8142e9a564d0bcda8ab78a2d5bf1ab655759a9a5348ef2",
                hash("Aarav Sharma", "Java", "A"));
    }

    @Test
    void editedRecipientChangesHash() {
        assertNotEquals(hash("Aarav Sharma", "Java", "A"), hash("Aarav Sharmaa", "Java", "A"));
    }

    @Test
    void editedCourseOrGradeChangesHash() {
        String original = hash("Aarav Sharma", "Java", "A");
        assertNotEquals(original, hash("Aarav Sharma", "Advanced Java", "A"));
        assertNotEquals(original, hash("Aarav Sharma", "Java", "A+"));
    }

    @Test
    void missingGradeIsTreatedAsEmpty() {
        assertEquals(hash("Aarav Sharma", "Java", null), hash("Aarav Sharma", "Java", ""));
    }

    @Test
    void expiryDateIsProtectedToo() {
        String noExpiry = CertificateHasher.compute("CERT-2026-000001", "A", "B", "C", ISSUED, "DEGREE", null, null);
        String withExpiry = CertificateHasher.compute("CERT-2026-000001", "A", "B", "C", ISSUED, "DEGREE", ISSUED.plusYears(2), null);
        assertNotEquals(noExpiry, withExpiry);
    }
}
