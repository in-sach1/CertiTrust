package com.digitalcertificate.dto;

import com.digitalcertificate.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin "add / edit user" form. Password may be blank when editing (= keep the current one). */
public class UserForm {
    @NotBlank(message = "Name is required") @Size(max = 100)
    private String name;
    @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 150)
    private String email;
    private String password;
    @NotNull(message = "Role is required")
    private Role role;
    private boolean enabled = true;
    private Long institutionId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Long getInstitutionId() { return institutionId; }
    public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }
}
