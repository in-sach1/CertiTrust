package com.digitalcertificate.controller.api;

import com.digitalcertificate.config.AppProperties;
import com.digitalcertificate.dto.ApiResponse;
import com.digitalcertificate.dto.CertificateDto;
import com.digitalcertificate.dto.CertificateRequest;
import com.digitalcertificate.dto.CertificateSearchCriteria;
import com.digitalcertificate.entity.CertificateType;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import com.digitalcertificate.dto.PageDto;
import com.digitalcertificate.dto.RevokeRequest;
import com.digitalcertificate.dto.VerificationDto;
import com.digitalcertificate.entity.Certificate;
import com.digitalcertificate.entity.Role;
import com.digitalcertificate.security.AppUserDetails;
import com.digitalcertificate.service.CertificateService;
import com.digitalcertificate.util.RequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for certificates. {certificateId} is the public ID (CERT-2026-000001), never the database id. */
@RestController
@RequestMapping("/api/certificates")
public class CertificateApiController {
    private final CertificateService certificateService;
    private final AppProperties props;
    private final RequestInfo requestInfo;

    public CertificateApiController(CertificateService certificateService, AppProperties props, RequestInfo requestInfo) {
        this.certificateService = certificateService;
        this.props = props;
        this.requestInfo = requestInfo;
    }

    /** Issuers see their own certificates; admins and verifiers can search all of them. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ISSUER','ADMIN','VERIFIER')")
    public ApiResponse<PageDto<CertificateDto>> list(@AuthenticationPrincipal AppUserDetails me,
                                                     @RequestParam(defaultValue = "") String q,
                                                     @RequestParam(defaultValue = "ALL") String filter,
                                                     @RequestParam(required = false) CertificateType type,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(defaultValue = "createdAt") String sort,
                                                     @RequestParam(defaultValue = "desc") String dir) {
        Long issuerId = me.getRole() == Role.ISSUER ? me.getId() : null;
        CertificateSearchCriteria criteria = new CertificateSearchCriteria();
        criteria.setQ(q);
        criteria.setFilter(filter);
        criteria.setType(type);
        criteria.setFrom(from);
        criteria.setTo(to);
        criteria.setPage(page);
        criteria.setSort(sort);
        criteria.setDir(dir);
        PageDto<CertificateDto> result = PageDto.from(
                certificateService.search(issuerId, criteria, size),
                c -> CertificateDto.from(c, props.baseUrl()));
        return ApiResponse.ok("Certificates retrieved", result);
    }

    @GetMapping("/{certificateId}")
    public ApiResponse<CertificateDto> get(@PathVariable String certificateId) {
        Certificate c = certificateService.getByCertificateId(certificateId);
        return ApiResponse.ok("Certificate found", CertificateDto.from(c, props.baseUrl()));
    }

    /** Verifies the certificate (counted and logged, exactly like the public /verify page). */
    @GetMapping("/{certificateId}/verify")
    public ApiResponse<VerificationDto> verify(@PathVariable String certificateId,
                                               @AuthenticationPrincipal AppUserDetails me,
                                               HttpServletRequest request) {
        VerificationDto dto = VerificationDto.from(
                certificateService.verify(certificateId, me == null ? null : me.getId(), requestInfo.ip(request)),
                props.baseUrl());
        return ApiResponse.ok("Verification completed", dto);
    }

    @PostMapping
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<CertificateDto>> create(@AuthenticationPrincipal AppUserDetails me,
                                                              @Valid @RequestBody CertificateRequest body,
                                                              HttpServletRequest request) {
        Certificate c = certificateService.issue(body, me.getId(), requestInfo.ip(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Certificate issued", CertificateDto.from(c, props.baseUrl())));
    }

    @PutMapping("/{certificateId}")
    @PreAuthorize("hasAnyRole('ISSUER','ADMIN')")
    public ApiResponse<CertificateDto> update(@PathVariable String certificateId,
                                              @AuthenticationPrincipal AppUserDetails me,
                                              @Valid @RequestBody CertificateRequest body,
                                              HttpServletRequest request) {
        Certificate c = certificateService.update(certificateId, body, me.getId(), requestInfo.ip(request));
        return ApiResponse.ok("Certificate updated", CertificateDto.from(c, props.baseUrl()));
    }

    @PostMapping("/{certificateId}/revoke")
    @PreAuthorize("hasAnyRole('ISSUER','ADMIN')")
    public ApiResponse<Void> revoke(@PathVariable String certificateId,
                                    @AuthenticationPrincipal AppUserDetails me,
                                    @Valid @RequestBody RevokeRequest body,
                                    HttpServletRequest request) {
        certificateService.revoke(certificateId, body.reason(), me.getId(), requestInfo.ip(request));
        return ApiResponse.ok("Certificate revoked", null);
    }
}
