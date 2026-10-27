package com.digitalcertificate.controller;

import com.digitalcertificate.entity.Certificate;
import com.digitalcertificate.entity.CertificateStatus;
import com.digitalcertificate.entity.Role;
import com.digitalcertificate.security.AppUserDetails;
import com.digitalcertificate.service.AuditService;
import com.digitalcertificate.service.CertificateService;
import com.digitalcertificate.util.RequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class CertificateController {
    private final CertificateService certificateService;
    private final AuditService audit;
    private final RequestInfo requestInfo;

    public CertificateController(CertificateService certificateService, AuditService audit, RequestInfo requestInfo) {
        this.certificateService = certificateService;
        this.audit = audit;
        this.requestInfo = requestInfo;
    }

    @GetMapping("/certificate/{certificateId}")
    public String details(@PathVariable String certificateId, @AuthenticationPrincipal AppUserDetails me,
                          HttpServletRequest request, Model model) {
        Certificate c = certificateService.getByCertificateId(certificateId);
        boolean privateView = me != null && (me.getRole() == Role.ADMIN
                || (me.getRole() == Role.ISSUER && c.getIssuer().getId().equals(me.getId())));
        audit.log("CERTIFICATE_VIEWED", me == null ? "anonymous" : me.getEmail(), c.getCertificateId(), requestInfo.ip(request));

        model.addAttribute("cert", c);
        model.addAttribute("integrity", certificateService.integrityOk(c));
        model.addAttribute("verifyUrl", certificateService.verificationUrl(c));
        model.addAttribute("canSeePrivate", privateView);
        model.addAttribute("canRevoke", privateView && c.getStatus() == CertificateStatus.ACTIVE);
        model.addAttribute("revocations", certificateService.revocationHistory(c));
        return "certificate/details";
    }

    @GetMapping("/certificate/{certificateId}/download")
    public ResponseEntity<Resource> download(@PathVariable String certificateId) {
        Certificate c = certificateService.getByCertificateId(certificateId);
        Resource pdf = new FileSystemResource(certificateService.pdfFile(c));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(c.getCertificateId() + ".pdf").build().toString())
                .body(pdf);
    }

    @GetMapping(value = "/certificate/{certificateId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] qr(@PathVariable String certificateId) {
        return certificateService.qrBytes(certificateService.getByCertificateId(certificateId));
    }
}
