package com.digitalcertificate.exception;

/** A well-formed certificate ID that does not exist in the records. */
public class CertificateNotFoundException extends ResourceNotFoundException {
    public CertificateNotFoundException(String certificateId) {
        super("Certificate not found");
    }
}
