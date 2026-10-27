package com.digitalcertificate.dto;

import com.digitalcertificate.entity.InstitutionStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InstitutionForm {
    @NotBlank(message = "Institution name is required") @Size(max = 150)
    private String name;
    @Size(max = 255)
    private String address;
    @Email(message = "Enter a valid email address") @Size(max = 150)
    private String email;
    @Size(max = 200)
    private String website;
    private InstitutionStatus status = InstitutionStatus.ACTIVE;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public InstitutionStatus getStatus() { return status; }
    public void setStatus(InstitutionStatus status) { this.status = status; }
}
