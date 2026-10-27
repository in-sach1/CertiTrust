package com.digitalcertificate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactForm {
    @NotBlank(message = "Please enter your name") @Size(max = 100)
    private String name;
    @NotBlank(message = "Please enter your email") @Email(message = "Enter a valid email address") @Size(max = 150)
    private String email;
    @NotBlank(message = "Please write a message") @Size(max = 300, message = "Message must be at most 300 characters")
    private String message;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
