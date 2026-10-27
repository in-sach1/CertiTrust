package com.digitalcertificate.dto;

import com.digitalcertificate.entity.VerificationLog;
import java.time.LocalDateTime;

public record VerificationLogDto(String certificateId, LocalDateTime verifiedAt, String status, String verifier) {

    public static VerificationLogDto from(VerificationLog l) {
        return new VerificationLogDto(l.getCertificateId(), l.getVerificationTime(), l.getVerificationStatus().name(),
                l.getVerifier() == null ? "Anonymous" : l.getVerifier().getName());
    }
}
