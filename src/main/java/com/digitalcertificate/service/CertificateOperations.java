package com.digitalcertificate.service;

import com.digitalcertificate.dto.CertificateRequest;
import com.digitalcertificate.dto.CertificateSearchCriteria;
import com.digitalcertificate.entity.Certificate;
import com.digitalcertificate.entity.CertificateType;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.util.List;

/** INTERFACE: the common operations on certificate records (issue, change, revoke, find, search). */
public interface CertificateOperations {
    Certificate issue(CertificateRequest request, Long issuerId, String ip);

    Certificate update(String certificateId, CertificateRequest request, Long actorId, String ip);

    void revoke(String certificateId, String reason, Long actorId, String ip);

    Certificate getByCertificateId(String certificateId);

    // METHOD OVERLOADING: one name, several ways to search
    Page<Certificate> search(Long issuerId, String q, String filter, int page, int size, String sort, String dir);

    Page<Certificate> search(Long issuerId, CertificateSearchCriteria criteria, int size);

    List<Certificate> search(String keyword);

    List<Certificate> search(CertificateType type);

    List<Certificate> search(LocalDate from, LocalDate to);
}
