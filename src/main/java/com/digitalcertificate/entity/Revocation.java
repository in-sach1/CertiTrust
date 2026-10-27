package com.digitalcertificate.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One row per revocation - certificates are never deleted, so this history stays available. */
@Entity
@Table(name = "revocations", indexes = @Index(name = "idx_rev_cert", columnList = "certificate_id"))
public class Revocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "certificate_id", nullable = false)
    private Certificate certificate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "revoked_by", nullable = false)
    private User revokedBy;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "revoked_at", nullable = false)
    private LocalDateTime revokedAt;

    public Long getId() { return id; }
    public Certificate getCertificate() { return certificate; }
    public void setCertificate(Certificate certificate) { this.certificate = certificate; }
    public User getRevokedBy() { return revokedBy; }
    public void setRevokedBy(User revokedBy) { this.revokedBy = revokedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
}
