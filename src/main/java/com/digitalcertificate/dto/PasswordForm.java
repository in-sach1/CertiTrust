package com.digitalcertificate.dto;

import com.digitalcertificate.util.Patterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class PasswordForm {
    @NotBlank(message = "Enter your current password")
    private String currentPassword;
    @NotBlank(message = "Choose a new password")
    @Pattern(regexp = Patterns.PASSWORD_REGEX, message = Patterns.PASSWORD_MESSAGE)
    private String newPassword;
    @NotBlank(message = "Confirm the new password")
    private String confirmPassword;

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
