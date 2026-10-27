package com.digitalcertificate.domain;

import com.digitalcertificate.entity.Role;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** INHERITANCE: an institution's certificate issuer. */
public class IssuerAccount extends Account {
    private static final Set<Permission> PERMISSIONS = Collections.unmodifiableSet(EnumSet.of(
            Permission.ISSUE_CERTIFICATES, Permission.REVOKE_CERTIFICATES, Permission.VERIFY_CERTIFICATES,
            Permission.SEARCH_CERTIFICATES, Permission.VIEW_HISTORY));

    private final String institutionName;

    public IssuerAccount(Long id, String name, String email, String institutionName) {
        super(id, name, email);
        this.institutionName = institutionName;
    }

    public String getInstitutionName() { return institutionName; }

    @Override public Role role() { return Role.ISSUER; }
    @Override public Set<Permission> permissions() { return PERMISSIONS; }
    @Override public String dashboardPath() { return "/issuer/dashboard"; }

    @Override
    public String describe() {
        return "Issuer " + getName() + (institutionName == null ? "" : " of " + institutionName)
                + " (issues and revokes certificates)";
    }
}
