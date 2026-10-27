package com.digitalcertificate.controller;

import com.digitalcertificate.exception.BusinessException;
import com.digitalcertificate.exception.ResourceNotFoundException;
import com.digitalcertificate.security.AppUserDetails;
import com.digitalcertificate.service.CertificateService;
import com.digitalcertificate.util.RequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;
import java.nio.charset.StandardCharsets;

/** Actions shared by issuers and admins (URL prefix /manage is limited to those two roles). */
@Controller
@RequestMapping("/manage")
public class ManageController {
    private final CertificateService certificateService;
    private final RequestInfo requestInfo;

    public ManageController(CertificateService certificateService, RequestInfo requestInfo) {
        this.certificateService = certificateService;
        this.requestInfo = requestInfo;
    }

    @PostMapping("/certificates/{certificateId}/revoke")
    public String revoke(@PathVariable String certificateId,
                         @RequestParam(defaultValue = "") String reason,
                         @RequestParam(defaultValue = "detail") String from,
                         @AuthenticationPrincipal AppUserDetails me,
                         HttpServletRequest request, RedirectAttributes redirect) {
        try {
            certificateService.revoke(certificateId, reason, me.getId(), requestInfo.ip(request));
            redirect.addFlashAttribute("flashSuccess", "Certificate " + certificateId.toUpperCase() + " has been revoked.");
        } catch (BusinessException | AccessDeniedException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("flashError", e instanceof AccessDeniedException
                    ? "You can only revoke certificates issued by you." : e.getMessage());
        }
        switch (from) {
            case "admin": return "redirect:/admin/certificates";
            case "issuer": return "redirect:/issuer/certificates";
            default: return "redirect:/certificate/" + UriUtils.encodePathSegment(certificateId, StandardCharsets.UTF_8);
        }
    }
}
