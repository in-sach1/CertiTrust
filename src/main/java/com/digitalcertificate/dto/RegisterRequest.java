package com.digitalcertificate.dto;

import com.digitalcertificate.entity.Role;
import com.digitalcertificate.util.Patterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Pattern(regexp = Patterns.PASSWORD_REGEX, message = Patterns.PASSWORD_MESSAGE) String password,
        @NotBlank String confirmPassword,
        @NotNull Role role,
        @Size(max = 150) String institutionName) { }
