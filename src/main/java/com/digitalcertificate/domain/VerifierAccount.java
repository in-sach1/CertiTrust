package com.digitalcertificate.domain;

import com.digitalcertificate.entity.Role;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** INHERITANCE: an employer / recruiter who checks certificates (cannot issue or revoke). */
public class VerifierAccount extends Account {
    private static final Set<Permission> PERMISSIONS = Collections.unmodifiableSet(EnumSet.of(
            Permission.VERIFY_CERTIFICATES, Permission.SEARCH_CERTIFICATES, Permission.VIEW_HISTORY));

    public VerifierAccount(Long id, String name, String email) {
        super(id, name, email);
    }

    @Override public Role role() { return Role.VERIFIER; }
    @Override public Set<Permission> permissions() { return PERMISSIONS; }
    @Override public String dashboardPath() { return "/verifier/dashboard"; }

    @Override
    public String describe() {
        return "Verifier " + getName() + " (verifies and searches certificates)";
    }
}
