package com.digitalcertificate.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificates", indexes = {
        @Index(name = "idx_cert_recipient", columnList = "recipient_name"),
        @Index(name = "idx_cert_status", columnList = "status"),
        @Index(name = "idx_cert_created", columnList = "created_at")})
public class Certificate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certificate_id", nullable = false, unique = true, length = 64)
    private String certificateId;

    @Column(name = "recipient_name", nullable = false, length = 120)
    private String recipientName;

    @Column(name = "recipient_email", nullable = false, length = 150)
    private String recipientEmail;

    @Column(nullable = false, length = 200)
    private String course;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", nullable = false, length = 30)
    private CertificateType certificateType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    /** Name of the institution at the time of issue (part of the hash, so later renames cannot break verification). */
    @Column(name = "institution_name", nullable = false, length = 150)
    private String institutionName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "issuer_id", nullable = false)
    private User issuer;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(length = 60)
    private String grade;

    @Column(name = "additional_info", length = 1000)
    private String additionalInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CertificateStatus status = CertificateStatus.ACTIVE;

    @Column(name = "certificate_hash", nullable = false, length = 64)
    private String certificateHash;

    @Column(name = "qr_code_path", length = 255)
    private String qrCodePath;

    @Column(name = "pdf_path", length = 255)
    private String pdfPath;

    @Column(name = "verification_count", nullable = false)
    private long verificationCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    /** VALID, REVOKED or EXPIRED - what users see in tables and badges. */
    public String getDisplayStatus() {
        if (status == CertificateStatus.REVOKED) {
            return "REVOKED";
        }
        return isExpired() ? "EXPIRED" : "VALID";
    }

    public String getShortHash() {
        return certificateHash == null || certificateHash.length() < 16
                ? certificateHash : certificateHash.substring(0, 8) + "…" + certificateHash.substring(certificateHash.length() - 8);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCertificateId() { return certificateId; }
    public void setCertificateId(String certificateId) { this.certificateId = certificateId; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    public CertificateType getCertificateType() { return certificateType; }
    public void setCertificateType(CertificateType certificateType) { this.certificateType = certificateType; }
    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }
    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }
    public User getIssuer() { return issuer; }
    public void setIssuer(User issuer) { this.issuer = issuer; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
    public CertificateStatus getStatus() { return status; }
    public void setStatus(CertificateStatus status) { this.status = status; }
    public String getCertificateHash() { return certificateHash; }
    public void setCertificateHash(String certificateHash) { this.certificateHash = certificateHash; }
    public String getQrCodePath() { return qrCodePath; }
    public void setQrCodePath(String qrCodePath) { this.qrCodePath = qrCodePath; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public long getVerificationCount() { return verificationCount; }
    public void setVerificationCount(long verificationCount) { this.verificationCount = verificationCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
