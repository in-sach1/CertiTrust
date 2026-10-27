package com.digitalcertificate.util;

import java.time.LocalDate;

/**
 * The certificate "fingerprint": SHA-256 over the important data. Kept free of framework classes so it is easy to test.
 * (The input layout must never change, otherwise certificates issued earlier would stop verifying.)
 */
public final class CertificateHasher {
    private CertificateHasher() { }

    public static String compute(String certificateId, String recipientName, String course, String institutionName,
                                 LocalDate issueDate, String certificateType, LocalDate expiryDate, String grade) {
        String data = String.join("|",
                certificateId, recipientName, course, institutionName,
                String.valueOf(issueDate), certificateType,
                expiryDate == null ? "" : expiryDate.toString(),
                grade == null ? "" : grade);
        return HashUtil.sha256Hex(data);
    }
}
