package com.digitalcertificate.domain;

import com.digitalcertificate.entity.Role;

/** Factory: turns a stored role into the matching Account subclass (POLYMORPHISM - callers only see Account). */
public final class Accounts {
    private Accounts() { }

    public static Account create(Long id, String name, String email, Role role, String institutionName) {
        switch (role) {
            case ADMIN: return new AdministratorAccount(id, name, email);
            case ISSUER: return new IssuerAccount(id, name, email, institutionName);
            default: return new VerifierAccount(id, name, email);
        }
    }
}
