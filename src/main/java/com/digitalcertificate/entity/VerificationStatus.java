package com.digitalcertificate.entity;

/** Result of one verification attempt. */
public enum VerificationStatus {
    VERIFIED("Verified Certificate", "This certificate is authentic and currently valid."),
    REVOKED("Certificate Revoked", "This certificate was issued by the institution but has since been revoked."),
    EXPIRED("Certificate Expired", "This certificate is authentic but its validity period has ended."),
    TAMPERED("Integrity Check Failed", "The stored data does not match this certificate's digital fingerprint. Do not trust it."),
    NOT_FOUND("Certificate Not Found / Invalid Certificate", "No certificate with this ID exists in our records.");

    private final String title;
    private final String message;

    VerificationStatus(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
}
