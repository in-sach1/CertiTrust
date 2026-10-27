package com.digitalcertificate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The text is not shaped like a certificate ID (expected CERT-YYYY-NNNNNN). */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCertificateIdException extends BusinessException {
    public InvalidCertificateIdException(String id) {
        super("'" + (id == null ? "" : (id.length() > 40 ? id.substring(0, 40) + "..." : id))
                + "' is not a valid certificate ID. IDs look like CERT-2026-000123.");
    }
}
