package com.digitalcertificate.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "institutions")
public class Institution {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(length = 150)
    private String email;

    @Column(length = 200)
    private String website;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstitutionStatus status = InstitutionStatus.ACTIVE;

    @Column(name = "logo_path", length = 150)
    private String logoPath;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
}
