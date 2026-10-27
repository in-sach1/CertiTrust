package com.digitalcertificate.service;

import com.digitalcertificate.entity.AuditLog;
import com.digitalcertificate.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Writes the audit trail: who did what, when, from where. */
@Service
public class AuditService {
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(String action, String actor, String details, String ip) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setActor(actor == null ? "anonymous" : truncate(actor, 150));
        entry.setDetails(truncate(details, 500));
        entry.setIpAddress(ip);
        repository.save(entry);
    }

    public Page<AuditLog> page(int page, int size) {
        return repository.findAll(PageRequest.of(Math.max(page, 0), size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    private static String truncate(String s, int max) {
        return s == null ? null : (s.length() <= max ? s : s.substring(0, max));
    }
}
