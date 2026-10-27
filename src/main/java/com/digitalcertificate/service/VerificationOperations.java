package com.digitalcertificate.service;

import com.digitalcertificate.dto.VerificationResult;
import com.digitalcertificate.entity.Certificate;

/** INTERFACE: how a certificate is verified. */
public interface VerificationOperations {
    VerificationResult verify(String certificateId, Long verifierId, String ip);

    /** OVERLOADING: anonymous verification (nobody logged in, no IP recorded). */
    VerificationResult verify(String certificateId);

    /** True when the stored data still matches the fingerprint created at issue time. */
    boolean isAuthentic(Certificate certificate);
}
