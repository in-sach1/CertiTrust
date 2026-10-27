package com.digitalcertificate.domain;

import com.digitalcertificate.entity.Role;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** INHERITANCE: the platform administrator. */
public class AdministratorAccount extends Account {
    private static final Set<Permission> PERMISSIONS = Collections.unmodifiableSet(EnumSet.of(
            Permission.MANAGE_USERS, Permission.MANAGE_INSTITUTIONS, Permission.REVOKE_CERTIFICATES,
            Permission.VERIFY_CERTIFICATES, Permission.SEARCH_CERTIFICATES, Permission.VIEW_HISTORY,
            Permission.VIEW_AUDIT_AND_REPORTS));

    public AdministratorAccount(Long id, String name, String email) {
        super(id, name, email);
    }

    @Override public Role role() { return Role.ADMIN; }
    @Override public Set<Permission> permissions() { return PERMISSIONS; }
    @Override public String dashboardPath() { return "/admin/dashboard"; }

    @Override
    public String describe() {
        return "Administrator " + getName() + " (manages users, institutions, logs and reports)";
    }
}
