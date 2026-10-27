package com.digitalcertificate.service;

import com.digitalcertificate.dto.CertificateRequest;
import com.digitalcertificate.dto.VerificationResult;
import com.digitalcertificate.entity.Certificate;
import com.digitalcertificate.entity.CertificateStatus;
import com.digitalcertificate.entity.Institution;
import com.digitalcertificate.entity.InstitutionStatus;
import com.digitalcertificate.entity.Revocation;
import com.digitalcertificate.entity.Role;
import com.digitalcertificate.entity.User;
import com.digitalcertificate.entity.VerificationLog;
import com.digitalcertificate.entity.VerificationStatus;
import com.digitalcertificate.exception.BusinessException;
import com.digitalcertificate.exception.ResourceNotFoundException;
import com.digitalcertificate.repository.CertificateRepository;
import com.digitalcertificate.repository.CertificateSpecs;
import com.digitalcertificate.repository.RevocationRepository;
import com.digitalcertificate.repository.UserRepository;
import com.digitalcertificate.repository.VerificationLogRepository;
import com.digitalcertificate.dto.CertificateSearchCriteria;
import com.digitalcertificate.entity.CertificateType;
import com.digitalcertificate.exception.CertificateNotFoundException;
import com.digitalcertificate.exception.InvalidCertificateIdException;
import com.digitalcertificate.exception.InvalidInputException;
import com.digitalcertificate.util.CertificateHasher;
import com.digitalcertificate.util.CertificateIdValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Core business logic: issue, update, revoke, verify and search certificates. */
@Service
public class CertificateService implements CertificateOperations, VerificationOperations {
    private static final Set<String> SORTABLE = Set.of(
            "certificateId", "recipientName", "course", "issueDate", "status", "verificationCount", "createdAt");

    private final CertificateRepository certificates;
    private final RevocationRepository revocations;
    private final VerificationLogRepository logs;
    private final UserRepository users;
    private final QrCodeService qr;
    private final PdfCertificateService pdf;
    private final StorageService storage;
    private final AuditService audit;

    public CertificateService(CertificateRepository certificates, RevocationRepository revocations,
                              VerificationLogRepository logs, UserRepository users, QrCodeService qr,
                              PdfCertificateService pdf, StorageService storage, AuditService audit) {
        this.certificates = certificates;
        this.revocations = revocations;
        this.logs = logs;
        this.users = users;
        this.qr = qr;
        this.pdf = pdf;
        this.storage = storage;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ issue / update / revoke

    @Override
    @Transactional
    public Certificate issue(CertificateRequest req, Long issuerId, String ip) {
        User issuer = users.findById(issuerId).orElseThrow(() -> new ResourceNotFoundException("Issuer not found"));
        if (issuer.getRole() != Role.ISSUER) {
            throw new AccessDeniedException("Only issuers can issue certificates");
        }
        Institution institution = issuer.getInstitution();
        if (institution == null) {
            throw new BusinessException("Your account is not linked to an institution. Please contact an administrator.");
        }
        if (institution.getStatus() != InstitutionStatus.ACTIVE) {
            throw new BusinessException("Your institution is disabled, so it cannot issue certificates.");
        }
        validateDates(req);

        Certificate c = new Certificate();
        c.setCertificateId("TMP-" + UUID.randomUUID());           // replaced below, once the database id is known
        c.setCertificateHash("pending");
        apply(c, req);
        c.setInstitution(institution);
        c.setInstitutionName(institution.getName());
        c.setIssuer(issuer);
        c.setStatus(CertificateStatus.ACTIVE);
        c = certificates.saveAndFlush(c);

        c.setCertificateId(String.format("CERT-%d-%06d", Year.now().getValue(), c.getId()));
        c.setCertificateHash(computeHash(c));
        c.setQrCodePath(qr.generate(c.getCertificateId()));
        c.setPdfPath(pdf.generate(c));
        c = certificates.save(c);

        audit.log("CERTIFICATE_CREATED", issuer.getEmail(), c.getCertificateId() + " issued to " + c.getRecipientName(), ip);
        return c;
    }

    /** Edits an active certificate. The hash, QR code and PDF are re-created so the record stays consistent. */
    @Override
    @Transactional
    public Certificate update(String certificateId, CertificateRequest req, Long actorId, String ip) {
        User actor = users.findById(actorId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Certificate c = getByCertificateId(certificateId);
        assertCanManage(c, actor);
        if (c.getStatus() == CertificateStatus.REVOKED) {
            throw new BusinessException("A revoked certificate cannot be modified.");
        }
        validateDates(req);
        apply(c, req);
        c.setCertificateHash(computeHash(c));
        c.setQrCodePath(qr.generate(c.getCertificateId()));
        c.setPdfPath(pdf.generate(c));
        c = certificates.save(c);
        audit.log("CERTIFICATE_UPDATED", actor.getEmail(), c.getCertificateId() + " updated", ip);
        return c;
    }

    @Override
    @Transactional
    public void revoke(String certificateId, String reason, Long actorId, String ip) {
        User actor = users.findById(actorId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Certificate c = getByCertificateId(certificateId);
        assertCanManage(c, actor);
        if (c.getStatus() == CertificateStatus.REVOKED) {
            throw new BusinessException("This certificate is already revoked.");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Please give a reason for the revocation.");
        }
        c.setStatus(CertificateStatus.REVOKED);
        certificates.save(c);

        Revocation r = new Revocation();
        r.setCertificate(c);
        r.setRevokedBy(actor);
        r.setReason(reason.trim().length() > 500 ? reason.trim().substring(0, 500) : reason.trim());
        r.setRevokedAt(LocalDateTime.now());
        revocations.save(r);

        audit.log("CERTIFICATE_REVOKED", actor.getEmail(), c.getCertificateId() + ": " + r.getReason(), ip);
    }

    private void assertCanManage(Certificate c, User actor) {
        boolean admin = actor.getRole() == Role.ADMIN;
        boolean owner = actor.getRole() == Role.ISSUER && c.getIssuer().getId().equals(actor.getId());
        if (!admin && !owner) {
            throw new AccessDeniedException("You can only manage certificates you issued");
        }
    }

    private void validateDates(CertificateRequest req) {
        if (req.getIssueDate().isAfter(LocalDate.now())) {
            throw new InvalidInputException("The issue date cannot be in the future.");
        }
        if (req.getExpiryDate() != null && !req.getExpiryDate().isAfter(req.getIssueDate())) {
            throw new InvalidInputException("The expiry date must be after the issue date.");
        }
    }

    private void apply(Certificate c, CertificateRequest req) {
        c.setRecipientName(req.getRecipientName().trim());
        c.setRecipientEmail(req.getRecipientEmail().trim().toLowerCase(Locale.ROOT));
        c.setCourse(req.getCourse().trim());
        c.setCertificateType(req.getCertificateType());
        c.setIssueDate(req.getIssueDate());
        c.setExpiryDate(req.getExpiryDate());
        c.setGrade(blankToNull(req.getGrade()));
        c.setAdditionalInfo(blankToNull(req.getAdditionalInfo()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ------------------------------------------------------------------ hash

    /** SHA-256 over the important certificate data. */
    public String computeHash(Certificate c) {
        return CertificateHasher.compute(c.getCertificateId(), c.getRecipientName(), c.getCourse(),
                c.getInstitutionName(), c.getIssueDate(), c.getCertificateType().name(), c.getExpiryDate(), c.getGrade());
    }

    @Override
    public boolean isAuthentic(Certificate certificate) {
        return integrityOk(certificate);
    }

    public boolean integrityOk(Certificate c) {
        return computeHash(c).equals(c.getCertificateHash());
    }

    // ------------------------------------------------------------------ verification

    @Override
    @Transactional
    public VerificationResult verify(String rawId, Long verifierId, String ip) {
        String id = rawId == null ? "" : rawId.trim().toUpperCase(Locale.ROOT);
        if (id.length() > 64) {
            id = id.substring(0, 64);
        }
        VerificationResult result = new VerificationResult();
        result.setCertificateId(id);
        result.setVerifiedAt(LocalDateTime.now());

        boolean validFormat = CertificateIdValidator.isValid(id);
        Certificate c = validFormat ? certificates.findByCertificateId(id).orElse(null) : null;
        if (c == null) {
            result.setStatus(VerificationStatus.NOT_FOUND);
            result.setInvalidFormat(!validFormat);
        } else {
            if (!integrityOk(c)) {
                result.setStatus(VerificationStatus.TAMPERED);
            } else if (c.getStatus() == CertificateStatus.REVOKED) {
                result.setStatus(VerificationStatus.REVOKED);
                result.setCertificate(c);
                result.setRevocation(revocations.findFirstByCertificateOrderByRevokedAtDesc(c).orElse(null));
            } else if (c.isExpired()) {
                result.setStatus(VerificationStatus.EXPIRED);
                result.setCertificate(c);
            } else {
                result.setStatus(VerificationStatus.VERIFIED);
                result.setCertificate(c);
            }
            c.setVerificationCount(c.getVerificationCount() + 1);
            certificates.save(c);
        }

        VerificationLog log = new VerificationLog();
        log.setCertificateId(id.isEmpty() ? "(empty)" : id);
        log.setVerificationTime(result.getVerifiedAt());
        log.setVerificationStatus(result.getStatus());
        log.setVerifier(verifierId == null ? null : users.getReferenceById(verifierId));
        log.setIpAddress(ip);
        logs.save(log);

        String actor = verifierId == null ? "anonymous" : users.findById(verifierId).map(User::getEmail).orElse("anonymous");
        audit.log("CERTIFICATE_VERIFIED", actor, log.getCertificateId() + " -> " + result.getStatus(), ip);
        return result;
    }

    // ------------------------------------------------------------------ queries

    @Override
    @Transactional(readOnly = true)
    public Certificate getByCertificateId(String certificateId) {
        if (!CertificateIdValidator.isValid(certificateId)) {
            throw new InvalidCertificateIdException(certificateId);
        }
        String id = certificateId.trim().toUpperCase(Locale.ROOT);
        return certificates.findByCertificateId(id).orElseThrow(() -> new CertificateNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Revocation> revocationHistory(Certificate c) {
        return revocations.findByCertificateOrderByRevokedAtDesc(c);
    }

    /** Classic search (kept for the REST API): builds the criteria object and delegates. */
    @Override
    @Transactional(readOnly = true)
    public Page<Certificate> search(Long issuerId, String q, String filter, int page, int size, String sort, String dir) {
        CertificateSearchCriteria c = new CertificateSearchCriteria();
        c.setQ(q);
        c.setFilter(filter);
        c.setPage(page);
        c.setSort(sort);
        c.setDir(dir);
        return search(issuerId, c, size);
    }

    /** Search + filter (status, type, issue-date range) + sort + paginate. issuerId == null means "all issuers". */
    @Override
    @Transactional(readOnly = true)
    public Page<Certificate> search(Long issuerId, CertificateSearchCriteria c, int size) {
        String property = SORTABLE.contains(c.getSort()) ? c.getSort() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(c.getDir()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(c.getPage(), Math.min(Math.max(size, 1), 100),
                Sort.by(direction, property).and(Sort.by(Sort.Direction.DESC, "id")));
        return certificates.findAll(
                CertificateSpecs.build(issuerId, c.getQ(), c.getFilter(), c.getType(), c.getFrom(), c.getTo()), pageable);
    }

    /** Quick keyword search over all certificates (newest 50). */
    @Override
    @Transactional(readOnly = true)
    public List<Certificate> search(String keyword) {
        return top50(CertificateSpecs.build(null, keyword, null));
    }

    /** All certificates of one type (newest 50). */
    @Override
    @Transactional(readOnly = true)
    public List<Certificate> search(CertificateType type) {
        return top50(CertificateSpecs.build(null, null, null, type, null, null));
    }

    /** All certificates issued between two dates, inclusive (newest 50). */
    @Override
    @Transactional(readOnly = true)
    public List<Certificate> search(LocalDate from, LocalDate to) {
        return top50(CertificateSpecs.build(null, null, null, null, from, to));
    }

    private List<Certificate> top50(org.springframework.data.jpa.domain.Specification<Certificate> spec) {
        return certificates.findAll(spec, PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt", "id"))).getContent();
    }

    /** Anonymous verification: nobody logged in, no IP recorded. */
    @Override
    @Transactional
    public VerificationResult verify(String certificateId) {
        return verify(certificateId, null, null);
    }

    // ------------------------------------------------------------------ files

    /** The PDF on disk; it is re-created automatically if the file went missing. */
    public Path pdfFile(Certificate c) {
        Path file = storage.certificate(c.getCertificateId() + ".pdf");
        if (!Files.exists(file)) {
            pdf.generate(c);
        }
        return file;
    }

    public byte[] qrBytes(Certificate c) {
        return qr.readOrCreate(c.getCertificateId());
    }

    public String verificationUrl(Certificate c) {
        return qr.verificationUrl(c.getCertificateId());
    }
}
