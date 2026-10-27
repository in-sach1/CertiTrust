package com.digitalcertificate.repository;

import com.digitalcertificate.entity.VerificationLog;
import com.digitalcertificate.entity.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {
    Page<VerificationLog> findByVerifierId(Long verifierId, Pageable pageable);

    long countByVerifierId(Long verifierId);

    long countByVerifierIdAndVerificationStatus(Long verifierId, VerificationStatus status);

    long countByVerificationStatus(VerificationStatus status);

    long countByVerificationTimeGreaterThanEqualAndVerificationTimeLessThan(LocalDateTime from, LocalDateTime to);

    @Query("select l from VerificationLog l where l.certificateId in "
            + "(select c.certificateId from Certificate c where c.issuer.id = :issuerId)")
    Page<VerificationLog> findForIssuer(@Param("issuerId") Long issuerId, Pageable pageable);

    @Modifying
    @Query("update VerificationLog l set l.verifier = null where l.verifier.id = :userId")
    int detachVerifier(@Param("userId") Long userId);
}
