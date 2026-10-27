package com.digitalcertificate.security;

import com.digitalcertificate.service.AuditService;
import com.digitalcertificate.util.RequestInfo;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

/** After login: write an audit entry and send everybody to /dashboard (which routes by role). */
@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final AuditService audit;
    private final RequestInfo requestInfo;

    public LoginSuccessHandler(AuditService audit, RequestInfo requestInfo) {
        super("/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
        this.audit = audit;
        this.requestInfo = requestInfo;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        audit.log("USER_LOGIN", authentication.getName(), "Signed in", requestInfo.ip(request));
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
