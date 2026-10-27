package com.digitalcertificate.repository;

import com.digitalcertificate.entity.Certificate;
import com.digitalcertificate.entity.Revocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RevocationRepository extends JpaRepository<Revocation, Long> {
    List<Revocation> findByCertificateOrderByRevokedAtDesc(Certificate certificate);

    Optional<Revocation> findFirstByCertificateOrderByRevokedAtDesc(Certificate certificate);

    boolean existsByRevokedById(Long userId);
}
