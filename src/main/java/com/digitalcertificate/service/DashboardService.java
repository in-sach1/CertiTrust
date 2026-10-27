package com.digitalcertificate.service;

import com.digitalcertificate.entity.AuditLog;
import com.digitalcertificate.entity.CertificateStatus;
import com.digitalcertificate.entity.InstitutionStatus;
import com.digitalcertificate.entity.Role;
import com.digitalcertificate.entity.VerificationLog;
import com.digitalcertificate.entity.VerificationStatus;
import com.digitalcertificate.repository.AuditLogRepository;
import com.digitalcertificate.repository.CertificateRepository;
import com.digitalcertificate.repository.CertificateSpecs;
import com.digitalcertificate.repository.InstitutionRepository;
import com.digitalcertificate.repository.UserRepository;
import com.digitalcertificate.repository.VerificationLogRepository;
import com.digitalcertificate.security.AppUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Numbers, chart data and "recent activity" lists for the landing page and the dashboards. */
@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final CertificateRepository certificates;
    private final VerificationLogRepository logs;
    private final UserRepository users;
    private final InstitutionRepository institutions;
    private final AuditLogRepository audits;

    public DashboardService(CertificateRepository certificates, VerificationLogRepository logs, UserRepository users,
                            InstitutionRepository institutions, AuditLogRepository audits) {
        this.certificates = certificates;
        this.logs = logs;
        this.users = users;
        this.institutions = institutions;
        this.audits = audits;
    }

    /** Live numbers for the landing page. */
    public Map<String, Object> publicStats() {
        long attempts = logs.count();
        long ok = logs.countByVerificationStatus(VerificationStatus.VERIFIED);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("issued", certificates.count());
        m.put("verified", attempts);
        m.put("institutions", institutions.countByStatus(InstitutionStatus.ACTIVE));
        m.put("successRate", attempts == 0 ? 100.0 : Math.round(ok * 1000.0 / attempts) / 10.0);
        return m;
    }

    public Map<String, Object> issuerStats(Long issuerId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("issued", certificates.count(CertificateSpecs.build(issuerId, null, null)));
        m.put("verified", certificates.sumVerificationsByIssuer(issuerId));
        m.put("active", certificates.count(CertificateSpecs.build(issuerId, null, "VALID")));
        m.put("revoked", certificates.count(CertificateSpecs.build(issuerId, null, "REVOKED")));
        m.put("expired", certificates.count(CertificateSpecs.build(issuerId, null, "EXPIRED")));
        return m;
    }

    public Map<String, Object> verifierStats(Long userId) {
        long total = logs.countByVerifierId(userId);
        long ok = logs.countByVerifierIdAndVerificationStatus(userId, VerificationStatus.VERIFIED);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("verified", ok);
        m.put("flagged", total - ok);
        return m;
    }

    public Map<String, Object> adminStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("users", users.count());
        m.put("certificates", certificates.count());
        m.put("revoked", certificates.count(CertificateSpecs.build(null, null, "REVOKED")));
        m.put("institutions", institutions.countByStatus(InstitutionStatus.ACTIVE));
        m.put("verifications", logs.count());
        return m;
    }

    /** Data for the four admin charts (plain lists/numbers, serialised to JSON by Thymeleaf). */
    public Map<String, Object> adminCharts() {
        DateTimeFormatter label = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
        List<String> months = new ArrayList<>();
        List<Long> issued = new ArrayList<>();
        List<Long> verified = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            LocalDateTime from = ym.atDay(1).atStartOfDay();
            LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();
            months.add(ym.format(label));
            issued.add(certificates.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to));
            verified.add(logs.countByVerificationTimeGreaterThanEqualAndVerificationTimeLessThan(from, to));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("months", months);
        m.put("issued", issued);
        m.put("verified", verified);
        m.put("statusLabels", List.of("Valid", "Revoked", "Expired"));
        m.put("statusCounts", List.of(
                certificates.count(CertificateSpecs.build(null, null, "VALID")),
                certificates.count(CertificateSpecs.build(null, null, "REVOKED")),
                certificates.count(CertificateSpecs.build(null, null, "EXPIRED"))));
        m.put("userLabels", List.of("Admins", "Issuers", "Verifiers"));
        m.put("userCounts", List.of(users.countByRole(Role.ADMIN), users.countByRole(Role.ISSUER), users.countByRole(Role.VERIFIER)));
        return m;
    }

    /** Verification history, limited to what the current user is allowed to see. */
    public Page<VerificationLog> history(AppUserDetails me, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "verificationTime", "id"));
        switch (me.getRole()) {
            case ADMIN: return logs.findAll(pageable);
            case ISSUER: return logs.findForIssuer(me.getId(), pageable);
            default: return logs.findByVerifierId(me.getId(), pageable);
        }
    }

    /** Short activity feed used by the bell menu and the "Recent activity" panels. */
    public List<Map<String, Object>> recentActivity(AppUserDetails me, int limit) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (me == null) {
            return items;
        }
        if (me.getRole() == Role.ADMIN) {
            Page<AuditLog> page = audits.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
            for (AuditLog a : page) {
                items.add(item(a.getAction().replace('_', ' ').toLowerCase(Locale.ENGLISH) + " - " + a.getActor(), a.getCreatedAt()));
            }
        } else {
            for (VerificationLog l : history(me, 0, limit)) {
                items.add(item(l.getCertificateId() + " - " + l.getVerificationStatus().getTitle(), l.getVerificationTime()));
            }
        }
        return items;
    }

    private static Map<String, Object> item(String text, LocalDateTime time) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("text", text);
        m.put("time", time);
        return m;
    }

    /** Used by the demo loader. */
    public static boolean isActive(CertificateStatus s) {
        return s == CertificateStatus.ACTIVE;
    }
}
