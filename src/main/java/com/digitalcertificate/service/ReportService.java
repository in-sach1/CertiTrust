package com.digitalcertificate.service;

import com.digitalcertificate.entity.Certificate;
import com.digitalcertificate.entity.CertificateType;
import com.digitalcertificate.entity.VerificationStatus;
import com.digitalcertificate.repository.CertificateRepository;
import com.digitalcertificate.repository.VerificationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** COLLECTIONS at work: groups and ranks certificate records in memory with List, Set and Map types. */
@Service
@Transactional(readOnly = true)
public class ReportService {
    private final CertificateRepository certificates;
    private final VerificationLogRepository logs;

    public ReportService(CertificateRepository certificates, VerificationLogRepository logs) {
        this.certificates = certificates;
        this.logs = logs;
    }

    /** Tables for the report page plus a "charts" entry that holds only plain labels and numbers. */
    public Map<String, Object> platformReport() {
        List<Certificate> all = certificates.findAll();                           // List

        Map<CertificateType, Long> byType = new EnumMap<>(CertificateType.class); // EnumMap
        for (CertificateType t : CertificateType.values()) {
            byType.put(t, 0L);
        }
        for (Certificate c : all) {
            byType.merge(c.getCertificateType(), 1L, Long::sum);
        }

        Map<String, Long> byInstitution = all.stream().collect(                   // TreeMap = sorted by name
                Collectors.groupingBy(Certificate::getInstitutionName, TreeMap::new, Collectors.counting()));

        Map<String, Long> byStatus = new LinkedHashMap<>();                       // LinkedHashMap = keeps order
        byStatus.put("VALID", 0L);
        byStatus.put("REVOKED", 0L);
        byStatus.put("EXPIRED", 0L);
        for (Certificate c : all) {
            byStatus.merge(c.getDisplayStatus(), 1L, Long::sum);
        }

        Map<VerificationStatus, Long> outcomes = new EnumMap<>(VerificationStatus.class);
        for (VerificationStatus s : VerificationStatus.values()) {
            outcomes.put(s, logs.countByVerificationStatus(s));
        }

        List<Certificate> top = all.stream()
                .sorted(Comparator.<Certificate>comparingLong(Certificate::getVerificationCount).reversed()
                        .thenComparing(Certificate::getCertificateId))
                .limit(5)
                .collect(Collectors.toList());

        Set<String> issuers = all.stream().map(c -> c.getIssuer().getName())      // Set = no duplicates
                .collect(Collectors.toCollection(TreeSet::new));

        List<String> typeLabels = new ArrayList<>();
        for (CertificateType t : byType.keySet()) {
            typeLabels.add(t.getLabel());
        }
        List<String> outcomeLabels = new ArrayList<>();
        for (VerificationStatus s : outcomes.keySet()) {
            outcomeLabels.add(s.name());
        }
        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("typeLabels", typeLabels);
        charts.put("typeCounts", new ArrayList<>(byType.values()));
        charts.put("outcomeLabels", outcomeLabels);
        charts.put("outcomeCounts", new ArrayList<>(outcomes.values()));

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("total", all.size());
        report.put("byType", byType);
        report.put("byInstitution", byInstitution);
        report.put("byStatus", byStatus);
        report.put("outcomes", outcomes);
        report.put("top", top);
        report.put("issuers", issuers);
        report.put("charts", charts);
        return report;
    }
}
