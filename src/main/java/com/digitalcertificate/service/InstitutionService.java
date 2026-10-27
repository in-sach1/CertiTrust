package com.digitalcertificate.service;

import com.digitalcertificate.dto.InstitutionForm;
import com.digitalcertificate.entity.Institution;
import com.digitalcertificate.entity.InstitutionStatus;
import com.digitalcertificate.exception.BusinessException;
import com.digitalcertificate.exception.ResourceNotFoundException;
import com.digitalcertificate.repository.InstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public class InstitutionService {
    private final InstitutionRepository institutions;
    private final StorageService storage;
    private final AuditService audit;

    public InstitutionService(InstitutionRepository institutions, StorageService storage, AuditService audit) {
        this.institutions = institutions;
        this.storage = storage;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Institution> findAll() {
        return institutions.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Institution get(Long id) {
        return institutions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Institution not found"));
    }

    @Transactional
    public Institution create(InstitutionForm f, MultipartFile logo, String adminEmail, String ip) {
        String name = f.getName().trim();
        if (institutions.existsByNameIgnoreCase(name)) {
            throw new BusinessException("An institution with this name already exists.");
        }
        Institution i = new Institution();
        copy(f, i);
        i.setName(name);
        i = institutions.save(i);
        i.setLogoPath(storage.saveLogo(i.getId(), logo, null));
        i = institutions.save(i);
        audit.log("INSTITUTION_CREATED", adminEmail, i.getName(), ip);
        return i;
    }

    @Transactional
    public Institution update(Long id, InstitutionForm f, MultipartFile logo, String adminEmail, String ip) {
        Institution i = get(id);
        String name = f.getName().trim();
        institutions.findByNameIgnoreCase(name).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new BusinessException("Another institution already uses this name.");
            }
        });
        copy(f, i);
        i.setName(name);
        i.setLogoPath(storage.saveLogo(i.getId(), logo, i.getLogoPath()));
        i = institutions.save(i);
        audit.log("INSTITUTION_UPDATED", adminEmail, i.getName(), ip);
        return i;
    }

    @Transactional
    public boolean toggle(Long id, String adminEmail, String ip) {
        Institution i = get(id);
        i.setStatus(i.getStatus() == InstitutionStatus.ACTIVE ? InstitutionStatus.DISABLED : InstitutionStatus.ACTIVE);
        institutions.save(i);
        audit.log(i.getStatus() == InstitutionStatus.ACTIVE ? "INSTITUTION_ENABLED" : "INSTITUTION_DISABLED", adminEmail, i.getName(), ip);
        return i.getStatus() == InstitutionStatus.ACTIVE;
    }

    private void copy(InstitutionForm f, Institution i) {
        String website = f.getWebsite() == null ? "" : f.getWebsite().trim();
        if (!website.isEmpty() && !website.matches("(?i)^https?://\\S+$")) {
            throw new BusinessException("The website must start with http:// or https://");
        }
        i.setAddress(blankToNull(f.getAddress()));
        i.setEmail(blankToNull(f.getEmail()));
        i.setWebsite(website.isEmpty() ? null : website);
        i.setStatus(f.getStatus() == null ? InstitutionStatus.ACTIVE : f.getStatus());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
