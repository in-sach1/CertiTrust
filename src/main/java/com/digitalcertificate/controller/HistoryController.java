package com.digitalcertificate.controller;

import com.digitalcertificate.domain.Account;
import com.digitalcertificate.domain.Accounts;
import com.digitalcertificate.domain.Permission;
import com.digitalcertificate.entity.Role;
import com.digitalcertificate.security.AppUserDetails;
import com.digitalcertificate.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Verification history / logs, scoped to what each role may see. */
@Controller
public class HistoryController {
    private final DashboardService dashboard;

    public HistoryController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/history")
    public String history(@AuthenticationPrincipal AppUserDetails me, Model model,
                          @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("page", dashboard.history(me, page, 15));
        Account account = Accounts.create(me.getId(), me.getName(), me.getEmail(), me.getRole(), null);
        model.addAttribute("showIp", account.can(Permission.VIEW_AUDIT_AND_REPORTS));
        model.addAttribute("scope", me.getRole() == Role.ADMIN ? "All verification attempts on the platform"
                : me.getRole() == Role.ISSUER ? "Verification attempts on certificates you issued"
                : "Certificates you have verified");
        return "history";
    }
}
