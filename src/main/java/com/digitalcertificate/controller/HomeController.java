package com.digitalcertificate.controller;

import com.digitalcertificate.dto.ContactForm;
import com.digitalcertificate.service.AuditService;
import com.digitalcertificate.service.DashboardService;
import com.digitalcertificate.util.RequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {
    private final DashboardService dashboard;
    private final AuditService audit;
    private final RequestInfo requestInfo;

    public HomeController(DashboardService dashboard, AuditService audit, RequestInfo requestInfo) {
        this.dashboard = dashboard;
        this.audit = audit;
        this.requestInfo = requestInfo;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("stats", dashboard.publicStats());
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/design")
    public String design() {
        return "design";
    }

    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("form", new ContactForm());
        return "contact";
    }

    /** Contact messages are stored in the audit log, where administrators can read them. */
    @PostMapping("/contact")
    public String send(@Valid @ModelAttribute("form") ContactForm form, BindingResult result,
                       HttpServletRequest request, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "contact";
        }
        audit.log("CONTACT_MESSAGE", form.getEmail(),
                form.getName() + ": " + form.getMessage().replaceAll("\\s+", " "), requestInfo.ip(request));
        redirect.addFlashAttribute("flashSuccess", "Thank you! Your message has been received.");
        return "redirect:/contact";
    }
}
