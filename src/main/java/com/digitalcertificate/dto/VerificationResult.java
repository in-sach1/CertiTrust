package com.digitalcertificate.dto;

import com.digitalcertificate.entity.Certificate;
import com.digitalcertificate.entity.Revocation;
import com.digitalcertificate.entity.VerificationStatus;
import java.time.LocalDateTime;

/** Outcome of one verification, shown on the /verify page. */
public class VerificationResult {
    private String certificateId;
    private VerificationStatus status;
    private Certificate certificate;   // only set when it is safe to display
    private Revocation revocation;
    private LocalDateTime verifiedAt;
    private boolean invalidFormat;    // the text typed in was not shaped like CERT-YYYY-NNNNNN

    public String getCertificateId() { return certificateId; }
    public void setCertificateId(String certificateId) { this.certificateId = certificateId; }
    public VerificationStatus getStatus() { return status; }
    public void setStatus(VerificationStatus status) { this.status = status; }
    public Certificate getCertificate() { return certificate; }
    public void setCertificate(Certificate certificate) { this.certificate = certificate; }
    public Revocation getRevocation() { return revocation; }
    public void setRevocation(Revocation revocation) { this.revocation = revocation; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public boolean isInvalidFormat() { return invalidFormat; }
    public void setInvalidFormat(boolean invalidFormat) { this.invalidFormat = invalidFormat; }
    public boolean isVerified() { return status == VerificationStatus.VERIFIED; }
}
