package com.digitalcertificate.controller;

import com.digitalcertificate.dto.CertificateSearchCriteria;
import com.digitalcertificate.entity.Certificate;
import com.digitalcertificate.entity.CertificateType;
import com.digitalcertificate.service.CertificateService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

/** Fills the model for templates/certificates/list.html (used by the issuer, admin and verifier pages). */
@Component
public class CertificateListSupport {
    private static final int PAGE_SIZE = 10;
    private final CertificateService certificateService;

    public CertificateListSupport(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    public String fill(Model model, Long issuerId, String mode, String basePath, String heading,
                       CertificateSearchCriteria criteria) {
        Page<Certificate> result = certificateService.search(issuerId, criteria, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("q", criteria.getQ());
        model.addAttribute("filter", criteria.getFilter());
        model.addAttribute("type", criteria.getType());
        model.addAttribute("from", criteria.getFrom());
        model.addAttribute("to", criteria.getTo());
        model.addAttribute("sort", criteria.getSort());
        model.addAttribute("dir", criteria.getDir());
        model.addAttribute("types", CertificateType.values());
        model.addAttribute("mode", mode);
        model.addAttribute("basePath", basePath);
        model.addAttribute("heading", heading);
        return "certificates/list";
    }
}
