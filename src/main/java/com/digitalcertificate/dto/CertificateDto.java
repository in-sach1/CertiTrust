package com.digitalcertificate.dto;

import com.digitalcertificate.entity.Certificate;
import java.time.LocalDate;

/** Public-safe view of a certificate (no e-mail address, no internal database id). */
public record CertificateDto(
        String certificateId, String recipientName, String course, String certificateType,
        String institution, String issuer, LocalDate issueDate, LocalDate expiryDate,
        String status, String grade, String hash, long verificationCount,
        String verifyUrl, String downloadUrl) {

    public static CertificateDto from(Certificate c, String baseUrl) {
        String base = baseUrl.replaceAll("/+$", "");
        return new CertificateDto(c.getCertificateId(), c.getRecipientName(), c.getCourse(),
                c.getCertificateType().getLabel(), c.getInstitutionName(), c.getIssuer().getName(),
                c.getIssueDate(), c.getExpiryDate(), c.getDisplayStatus(), c.getGrade(),
                c.getCertificateHash(), c.getVerificationCount(),
                base + "/verify?certificateId=" + c.getCertificateId(),
                base + "/certificate/" + c.getCertificateId() + "/download");
    }
}
