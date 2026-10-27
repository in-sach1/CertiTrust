package com.digitalcertificate.dto;

import com.digitalcertificate.entity.CertificateType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

/** Used both by the "Issue Certificate" form and by POST/PUT /api/certificates. */
public class CertificateRequest {
    @NotBlank @Size(max = 120)
    private String recipientName;
    @NotBlank @Email @Size(max = 150)
    private String recipientEmail;
    @NotBlank @Size(max = 200)
    private String course;
    @NotNull
    private CertificateType certificateType;
    @NotNull @PastOrPresent
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate issueDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;
    @Size(max = 60)
    private String grade;
    @Size(max = 1000)
    private String additionalInfo;

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    public CertificateType getCertificateType() { return certificateType; }
    public void setCertificateType(CertificateType certificateType) { this.certificateType = certificateType; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
}
