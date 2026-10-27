package com.digitalcertificate.util;

import com.digitalcertificate.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** Reads the client IP only when logging IPs is enabled (app.log-ip). */
@Component
public class RequestInfo {
    private final AppProperties props;

    public RequestInfo(AppProperties props) {
        this.props = props;
    }

    public String ip(HttpServletRequest request) {
        return props.logIp() ? request.getRemoteAddr() : null;
    }
}
