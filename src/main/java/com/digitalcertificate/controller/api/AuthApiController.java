package com.digitalcertificate.controller.api;

import com.digitalcertificate.dto.ApiResponse;
import com.digitalcertificate.dto.LoginRequest;
import com.digitalcertificate.dto.RegisterRequest;
import com.digitalcertificate.dto.UserDto;
import com.digitalcertificate.entity.User;
import com.digitalcertificate.exception.BusinessException;
import com.digitalcertificate.security.AppUserDetails;
import com.digitalcertificate.service.AuditService;
import com.digitalcertificate.service.UserService;
import com.digitalcertificate.util.RequestInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final AuditService audit;
    private final RequestInfo requestInfo;

    public AuthApiController(UserService userService, AuthenticationManager authenticationManager,
                             AuditService audit, RequestInfo requestInfo) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.audit = audit;
        this.requestInfo = requestInfo;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http) {
        if (!req.password().equals(req.confirmPassword())) {
            throw new BusinessException("Passwords do not match");
        }
        User u = userService.register(req.name(), req.email(), req.password(), req.role(),
                req.institutionName(), requestInfo.ip(http));
        String message = u.isEnabled() ? "Registration successful"
                : "Registration received - an administrator must approve this issuer account before it can sign in";
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(message, UserDto.from(u)));
    }

    /** Starts a session (JSESSIONID cookie). For stateless use, send HTTP Basic credentials instead. */
    @PostMapping("/login")
    public ApiResponse<UserDto> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http, HttpServletResponse res) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim(), req.password()));
        if (http.getSession(false) != null) {
            http.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, http, res);

        AppUserDetails principal = (AppUserDetails) auth.getPrincipal();
        audit.log("USER_LOGIN", principal.getEmail(), "Signed in via API", requestInfo.ip(http));
        return ApiResponse.ok("Login successful", UserDto.from(userService.getById(principal.getId())));
    }
}
