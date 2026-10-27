package com.digitalcertificate.repository;

import com.digitalcertificate.entity.Certificate;
import com.digitalcertificate.entity.CertificateStatus;
import com.digitalcertificate.entity.CertificateType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Builds the dynamic search / filter query for certificate tables. */
public final class CertificateSpecs {
    private CertificateSpecs() { }

    /** Search without type / date filters. */
    public static Specification<Certificate> build(Long issuerId, String q, String filter) {
        return build(issuerId, q, filter, null, null, null);
    }

    /**
     * @param issuerId only certificates of this issuer (null = all)
     * @param q        free text: certificate ID, recipient, course or institution
     * @param filter   VALID, REVOKED, EXPIRED (anything else = no filter)
     */
    public static Specification<Certificate> build(Long issuerId, String q, String filter,
                                                   CertificateType type, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (issuerId != null) {
                p.add(cb.equal(root.get("issuer").get("id"), issuerId));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.<String>get("certificateId")), like),
                        cb.like(cb.lower(root.<String>get("recipientName")), like),
                        cb.like(cb.lower(root.<String>get("course")), like),
                        cb.like(cb.lower(root.<String>get("institutionName")), like)));
            }
            if (type != null) {
                p.add(cb.equal(root.get("certificateType"), type));
            }
            if (from != null) {
                p.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("issueDate"), from));
            }
            if (to != null) {
                p.add(cb.lessThanOrEqualTo(root.<LocalDate>get("issueDate"), to));
            }
            String f = filter == null ? "" : filter.toUpperCase();
            if (f.equals("VALID")) {
                p.add(cb.equal(root.get("status"), CertificateStatus.ACTIVE));
                p.add(cb.or(cb.isNull(root.get("expiryDate")),
                        cb.greaterThanOrEqualTo(root.<LocalDate>get("expiryDate"), today)));
            } else if (f.equals("REVOKED")) {
                p.add(cb.equal(root.get("status"), CertificateStatus.REVOKED));
            } else if (f.equals("EXPIRED")) {
                p.add(cb.equal(root.get("status"), CertificateStatus.ACTIVE));
                p.add(cb.lessThan(root.<LocalDate>get("expiryDate"), today));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }
}
