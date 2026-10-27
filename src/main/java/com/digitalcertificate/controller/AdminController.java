package com.digitalcertificate.controller;

import com.digitalcertificate.dto.CertificateSearchCriteria;
import com.digitalcertificate.dto.InstitutionForm;
import com.digitalcertificate.dto.UserForm;
import com.digitalcertificate.entity.InstitutionStatus;
import com.digitalcertificate.entity.Role;
import com.digitalcertificate.exception.BusinessException;
import com.digitalcertificate.exception.ResourceNotFoundException;
import com.digitalcertificate.security.AppUserDetails;
import com.digitalcertificate.service.AuditService;
import com.digitalcertificate.service.DashboardService;
import com.digitalcertificate.service.InstitutionService;
import com.digitalcertificate.service.ReportService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Everything under /admin (the whole prefix is restricted to the ADMIN role in SecurityConfig). */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final DashboardService dashboard;
    private final UserService userService;
    private final InstitutionService institutionService;
    private final AuditService auditService;
    private final CertificateListSupport listSupport;
    private final RequestInfo requestInfo;
    private final ReportService reportService;

    public AdminController(DashboardService dashboard, UserService userService, InstitutionService institutionService,
                           AuditService auditService, CertificateListSupport listSupport, RequestInfo requestInfo,
                           ReportService reportService) {
        this.dashboard = dashboard;
        this.userService = userService;
        this.institutionService = institutionService;
        this.auditService = auditService;
        this.listSupport = listSupport;
        this.requestInfo = requestInfo;
        this.reportService = reportService;
    }

    // ------------------------------------------------------------------ dashboard & certificates

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("stats", dashboard.adminStats());
        model.addAttribute("charts", dashboard.adminCharts());
        model.addAttribute("recent", dashboard.recentActivity(me, 8));
        return "admin/dashboard";
    }

    @GetMapping("/certificates")
    public String certificates(Model model, CertificateSearchCriteria criteria, BindingResult binding) {
        return listSupport.fill(model, null, "ADMIN", "/admin/certificates", "All Certificates", criteria);
    }

    @GetMapping("/audit")
    public String audit(Model model, @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("page", auditService.page(page, 20));
        return "admin/audit";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        java.util.Map<String, Object> report = reportService.platformReport();
        model.addAttribute("report", report);
        model.addAttribute("charts", report.get("charts"));
        return "admin/reports";
    }

    // ------------------------------------------------------------------ users

    @GetMapping("/users")
    public String users(Model model, @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("page", userService.search(q, page, 10));
        model.addAttribute("q", q);
        model.addAttribute("roles", Role.values());
        model.addAttribute("institutions", institutionService.findAll());
        return "admin/users";
    }

    @PostMapping("/users")
    public String createUser(@AuthenticationPrincipal AppUserDetails me, @Valid @ModelAttribute UserForm form,
                             BindingResult result, HttpServletRequest request, RedirectAttributes redirect) {
        return run(result, redirect, "/admin/users", "User created.",
                () -> userService.createByAdmin(form, me.getEmail(), requestInfo.ip(request)));
    }

    @PostMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me,
                           @Valid @ModelAttribute UserForm form, BindingResult result,
                           HttpServletRequest request, RedirectAttributes redirect) {
        return run(result, redirect, "/admin/users", "User updated.",
                () -> userService.updateByAdmin(id, form, me.getId(), me.getEmail(), requestInfo.ip(request)));
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me,
                             HttpServletRequest request, RedirectAttributes redirect) {
        return run(null, redirect, "/admin/users", "User status changed.",
                () -> userService.toggleEnabled(id, me.getId(), me.getEmail(), requestInfo.ip(request)));
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me,
                             HttpServletRequest request, RedirectAttributes redirect) {
        return run(null, redirect, "/admin/users", "User deleted.",
                () -> userService.delete(id, me.getId(), me.getEmail(), requestInfo.ip(request)));
    }

    // ------------------------------------------------------------------ institutions

    @GetMapping("/institutions")
    public String institutions(Model model) {
        model.addAttribute("institutions", institutionService.findAll());
        model.addAttribute("statuses", InstitutionStatus.values());
        return "admin/institutions";
    }

    @PostMapping("/institutions")
    public String createInstitution(@AuthenticationPrincipal AppUserDetails me,
                                    @Valid @ModelAttribute InstitutionForm form, BindingResult result,
                                    @RequestParam(required = false) MultipartFile logo,
                                    HttpServletRequest request, RedirectAttributes redirect) {
        return run(result, redirect, "/admin/institutions", "Institution added.",
                () -> institutionService.create(form, logo, me.getEmail(), requestInfo.ip(request)));
    }

    @PostMapping("/institutions/{id}/edit")
    public String editInstitution(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me,
                                  @Valid @ModelAttribute InstitutionForm form, BindingResult result,
                                  @RequestParam(required = false) MultipartFile logo,
                                  HttpServletRequest request, RedirectAttributes redirect) {
        return run(result, redirect, "/admin/institutions", "Institution updated.",
                () -> institutionService.update(id, form, logo, me.getEmail(), requestInfo.ip(request)));
    }

    @PostMapping("/institutions/{id}/toggle")
    public String toggleInstitution(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me,
                                    HttpServletRequest request, RedirectAttributes redirect) {
        return run(null, redirect, "/admin/institutions", "Institution status changed.",
                () -> institutionService.toggle(id, me.getEmail(), requestInfo.ip(request)));
    }

    // ------------------------------------------------------------------ helper

    /** Validates, runs the action, and turns the outcome into a flash message + redirect. */
    private String run(BindingResult result, RedirectAttributes redirect, String target, String success, Runnable action) {
        if (result != null && result.hasErrors()) {
            redirect.addFlashAttribute("flashError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:" + target;
        }
        try {
            action.run();
            redirect.addFlashAttribute("flashSuccess", success);
        } catch (BusinessException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:" + target;
    }
}
