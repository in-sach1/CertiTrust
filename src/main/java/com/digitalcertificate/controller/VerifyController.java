package com.digitalcertificate.controller;

import com.digitalcertificate.security.AppUserDetails;
import com.digitalcertificate.service.CertificateService;
import com.digitalcertificate.util.RequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Public verification page. QR codes open /verify?certificateId=... */
@Controller
public class VerifyController {
    private final CertificateService certificateService;
    private final RequestInfo requestInfo;

    public VerifyController(CertificateService certificateService, RequestInfo requestInfo) {
        this.certificateService = certificateService;
        this.requestInfo = requestInfo;
    }

    @GetMapping("/verify")
    public String verify(@RequestParam(required = false) String certificateId,
                         @AuthenticationPrincipal AppUserDetails me,
                         HttpServletRequest request, Model model) {
        String id = certificateId == null ? "" : certificateId.trim();
        model.addAttribute("certificateId", id);
        if (!id.isEmpty()) {
            model.addAttribute("result",
                    certificateService.verify(id, me == null ? null : me.getId(), requestInfo.ip(request)));
        }
        return "verify";
    }
}
