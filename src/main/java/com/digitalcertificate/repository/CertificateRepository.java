package com.digitalcertificate.repository;

import com.digitalcertificate.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long>, JpaSpecificationExecutor<Certificate> {
    Optional<Certificate> findByCertificateId(String certificateId);

    boolean existsByIssuerId(Long issuerId);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);

    @Query("select coalesce(sum(c.verificationCount), 0) from Certificate c where c.issuer.id = :issuerId")
    long sumVerificationsByIssuer(@Param("issuerId") Long issuerId);
}
