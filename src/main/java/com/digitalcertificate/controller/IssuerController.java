package com.digitalcertificate.controller;

import com.digitalcertificate.dto.CertificateRequest;
import com.digitalcertificate.dto.CertificateSearchCriteria;
import com.digitalcertificate.entity.Certificate;
import com.digitalcertificate.entity.CertificateType;
import com.digitalcertificate.entity.Institution;
import com.digitalcertificate.exception.BusinessException;
import com.digitalcertificate.security.AppUserDetails;
import com.digitalcertificate.service.CertificateService;
import com.digitalcertificate.service.DashboardService;
import com.digitalcertificate.service.UserService;
import com.digitalcertificate.util.RequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;

@Controller
@RequestMapping("/issuer")
public class IssuerController {
    private final DashboardService dashboard;
    private final CertificateService certificateService;
    private final UserService userService;
    private final CertificateListSupport listSupport;
    private final RequestInfo requestInfo;

    public IssuerController(DashboardService dashboard, CertificateService certificateService, UserService userService,
                            CertificateListSupport listSupport, RequestInfo requestInfo) {
        this.dashboard = dashboard;
        this.certificateService = certificateService;
        this.userService = userService;
        this.listSupport = listSupport;
        this.requestInfo = requestInfo;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("stats", dashboard.issuerStats(me.getId()));
        model.addAttribute("recent", dashboard.recentActivity(me, 8));
        return "issuer/dashboard";
    }

    @GetMapping("/issue")
    public String issueForm(@AuthenticationPrincipal AppUserDetails me, Model model) {
        CertificateRequest form = new CertificateRequest();
        form.setIssueDate(LocalDate.now());
        form.setCertificateType(CertificateType.COURSE_COMPLETION);
        model.addAttribute("form", form);
        prepareIssueModel(me, model);
        return "issuer/issue";
    }

    @PostMapping("/issue")
    public String issue(@AuthenticationPrincipal AppUserDetails me,
                        @Valid @ModelAttribute("form") CertificateRequest form, BindingResult result,
                        HttpServletRequest request, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                Certificate c = certificateService.issue(form, me.getId(), requestInfo.ip(request));
                redirect.addFlashAttribute("flashSuccess", "Certificate " + c.getCertificateId()
                        + " was issued. The QR code and PDF were generated automatically.");
                return "redirect:/certificate/" + c.getCertificateId();
            } catch (BusinessException e) {
                result.reject("issue", e.getMessage());
            }
        }
        prepareIssueModel(me, model);
        return "issuer/issue";
    }

    @GetMapping("/certificates")
    public String certificates(@AuthenticationPrincipal AppUserDetails me, Model model,
                               CertificateSearchCriteria criteria, BindingResult binding) {
        return listSupport.fill(model, me.getId(), "ISSUER", "/issuer/certificates", "My Certificates", criteria);
    }

    private void prepareIssueModel(AppUserDetails me, Model model) {
        Institution inst = userService.getById(me.getId()).getInstitution();
        model.addAttribute("types", CertificateType.values());
        model.addAttribute("institutionName", inst == null ? "(no institution assigned)" : inst.getName());
        model.addAttribute("today", LocalDate.now());
    }
}
