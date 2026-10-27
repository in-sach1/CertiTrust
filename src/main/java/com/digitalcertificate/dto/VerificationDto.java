package com.digitalcertificate.dto;

import java.time.LocalDateTime;

public record VerificationDto(String certificateId, String status, String message, LocalDateTime verifiedAt,
                              CertificateDto certificate, LocalDateTime revokedAt, String revocationReason) {

    public static VerificationDto from(VerificationResult r, String baseUrl) {
        return new VerificationDto(r.getCertificateId(), r.getStatus().name(), r.getStatus().getMessage(),
                r.getVerifiedAt(),
                r.getCertificate() == null ? null : CertificateDto.from(r.getCertificate(), baseUrl),
                r.getRevocation() == null ? null : r.getRevocation().getRevokedAt(),
                r.getRevocation() == null ? null : r.getRevocation().getReason());
    }
}
