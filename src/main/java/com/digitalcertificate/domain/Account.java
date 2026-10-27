package com.digitalcertificate.domain;

import com.digitalcertificate.entity.Role;
import java.util.Locale;
import java.util.Set;

/**
 * ABSTRACTION + ENCAPSULATION: what every kind of account has in common.
 * Subclasses (administrator, issuer, verifier) INHERIT the fields and OVERRIDE the role-specific behaviour.
 */
public abstract class Account {
    private final Long id;
    private final String name;
    private final String email;

    protected Account(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    /** Role-specific (overridden in every subclass). */
    public abstract Role role();

    public abstract Set<Permission> permissions();

    public abstract String dashboardPath();

    /** METHOD OVERLOADING: check a permission by enum ... */
    public final boolean can(Permission permission) {
        return permission != null && permissions().contains(permission);
    }

    /** ... or by its name, e.g. can("ISSUE_CERTIFICATES"). Unknown names are simply "no". */
    public final boolean can(String permissionName) {
        if (permissionName == null) {
            return false;
        }
        try {
            return can(Permission.valueOf(permissionName.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String describe() {
        return role() + " account of " + name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return describe();
    }
}
