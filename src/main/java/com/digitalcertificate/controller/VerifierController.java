package com.digitalcertificate.controller;

import com.digitalcertificate.dto.CertificateSearchCriteria;
import com.digitalcertificate.security.AppUserDetails;
import com.digitalcertificate.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/verifier")
public class VerifierController {
    private final DashboardService dashboard;
    private final CertificateListSupport listSupport;

    public VerifierController(DashboardService dashboard, CertificateListSupport listSupport) {
        this.dashboard = dashboard;
        this.listSupport = listSupport;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("stats", dashboard.verifierStats(me.getId()));
        model.addAttribute("recent", dashboard.recentActivity(me, 8));
        return "verifier/dashboard";
    }

    /** Verifiers can search every certificate, but cannot issue or revoke. */
    @GetMapping("/certificates")
    public String search(Model model, CertificateSearchCriteria criteria, BindingResult binding) {
        return listSupport.fill(model, null, "VERIFIER", "/verifier/certificates", "Search Certificates", criteria);
    }
}
