package com.digitalcertificate.security;

import com.digitalcertificate.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

/** 401 / 403 responses of the REST API as JSON (instead of a login redirect or an HTML page). */
@Component
public class JsonAuthEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper;

    public JsonAuthEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required. Please log in.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN, "You do not have permission to perform this action.");
    }

    private void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), ApiResponse.error(message));
    }
}
