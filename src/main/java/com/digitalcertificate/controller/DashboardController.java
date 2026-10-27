package com.digitalcertificate.controller;

import com.digitalcertificate.domain.Account;
import com.digitalcertificate.domain.Accounts;
import com.digitalcertificate.security.AppUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    /** After login everybody lands here and is sent to the dashboard of their role. */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserDetails me) {
        // POLYMORPHISM: every Account subclass knows its own dashboard
        Account account = Accounts.create(me.getId(), me.getName(), me.getEmail(), me.getRole(), null);
        return "redirect:" + account.dashboardPath();
    }
}
