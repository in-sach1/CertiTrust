package com.digitalcertificate.controller.api;

import com.digitalcertificate.dto.ApiResponse;
import com.digitalcertificate.dto.PageDto;
import com.digitalcertificate.dto.VerificationLogDto;
import com.digitalcertificate.security.AppUserDetails;
import com.digitalcertificate.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verification")
public class VerificationApiController {
    private final DashboardService dashboard;

    public VerificationApiController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    /** Verifiers: their own checks. Issuers: checks on their certificates. Admins: everything. */
    @GetMapping("/history")
    public ApiResponse<PageDto<VerificationLogDto>> history(@AuthenticationPrincipal AppUserDetails me,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Verification history retrieved",
                PageDto.from(dashboard.history(me, page, Math.min(Math.max(size, 1), 100)), VerificationLogDto::from));
    }
}
