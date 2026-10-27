package com.digitalcertificate.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_logs", indexes = {
        @Index(name = "idx_vlog_cert", columnList = "certificate_id"),
        @Index(name = "idx_vlog_time", columnList = "verification_time")})
public class VerificationLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The certificate ID that was checked (kept as text so failed look-ups are logged too). */
    @Column(name = "certificate_id", nullable = false, length = 64)
    private String certificateId;

    @Column(name = "verification_time", nullable = false)
    private LocalDateTime verificationTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    /** Null = anonymous (not logged in). */
    @ManyToOne
    @JoinColumn(name = "verifier_id")
    private User verifier;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    public Long getId() { return id; }
    public String getCertificateId() { return certificateId; }
    public void setCertificateId(String certificateId) { this.certificateId = certificateId; }
    public LocalDateTime getVerificationTime() { return verificationTime; }
    public void setVerificationTime(LocalDateTime verificationTime) { this.verificationTime = verificationTime; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public User getVerifier() { return verifier; }
    public void setVerifier(User verifier) { this.verifier = verifier; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
