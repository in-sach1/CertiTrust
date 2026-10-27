package com.digitalcertificate.repository;

import com.digitalcertificate.entity.Institution;
import com.digitalcertificate.entity.InstitutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {
    Optional<Institution> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    long countByStatus(InstitutionStatus status);

    List<Institution> findAllByOrderByNameAsc();
}
