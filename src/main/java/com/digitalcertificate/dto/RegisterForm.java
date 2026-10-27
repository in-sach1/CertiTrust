package com.digitalcertificate.dto;

import com.digitalcertificate.entity.Role;
import com.digitalcertificate.util.Patterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterForm {
    @NotBlank(message = "Please enter your full name") @Size(max = 100)
    private String name;
    @NotBlank(message = "Please enter your email") @Email(message = "Enter a valid email address") @Size(max = 150)
    private String email;
    @NotBlank(message = "Please choose a password")
    @Pattern(regexp = Patterns.PASSWORD_REGEX, message = Patterns.PASSWORD_MESSAGE)
    private String password;
    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;
    @NotNull(message = "Please choose a role")
    private Role role = Role.VERIFIER;
    @Size(max = 150)
    private String institutionName;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }
}
