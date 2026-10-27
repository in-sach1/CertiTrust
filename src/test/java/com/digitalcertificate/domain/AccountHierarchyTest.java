package com.digitalcertificate.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.digitalcertificate.entity.Role;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Inheritance, overriding and overloading of the role classes. */
class AccountHierarchyTest {

    @Test
    void factoryCreatesTheMatchingSubclass() {
        assertTrue(Accounts.create(1L, "Ann", "a@x.com", Role.ADMIN, null) instanceof AdministratorAccount);
        assertTrue(Accounts.create(2L, "Ian", "i@x.com", Role.ISSUER, "DIT") instanceof IssuerAccount);
        assertTrue(Accounts.create(3L, "Vera", "v@x.com", Role.VERIFIER, null) instanceof VerifierAccount);
    }

    @Test
    void eachRoleOverridesTheDashboardPath() {
        assertEquals("/admin/dashboard", Accounts.create(1L, "A", "a", Role.ADMIN, null).dashboardPath());
        assertEquals("/issuer/dashboard", Accounts.create(2L, "I", "i", Role.ISSUER, null).dashboardPath());
        assertEquals("/verifier/dashboard", Accounts.create(3L, "V", "v", Role.VERIFIER, null).dashboardPath());
    }

    @Test
    void verifierCannotIssueOrRevoke() {
        Account verifier = Accounts.create(3L, "V", "v", Role.VERIFIER, null);
        assertTrue(verifier.can(Permission.VERIFY_CERTIFICATES));
        assertFalse(verifier.can(Permission.ISSUE_CERTIFICATES));
        assertFalse(verifier.can(Permission.REVOKE_CERTIFICATES));
    }

    @Test
    void issuerCanIssueButNotManageUsers() {
        Account issuer = Accounts.create(2L, "I", "i", Role.ISSUER, "DIT");
        assertTrue(issuer.can(Permission.ISSUE_CERTIFICATES));
        assertFalse(issuer.can(Permission.MANAGE_USERS));
    }

    @Test
    void adminManagesUsersAndSeesReports() {
        Account admin = Accounts.create(1L, "A", "a", Role.ADMIN, null);
        assertTrue(admin.can(Permission.MANAGE_USERS));
        assertTrue(admin.can(Permission.VIEW_AUDIT_AND_REPORTS));
    }

    @Test
    void overloadedCanAcceptsPermissionNames() {
        Account issuer = Accounts.create(2L, "I", "i", Role.ISSUER, null);
        assertTrue(issuer.can("issue_certificates"));
        assertFalse(issuer.can("MANAGE_USERS"));
        assertFalse(issuer.can("NO_SUCH_PERMISSION"));
        assertFalse(issuer.can((String) null));
    }

    @Test
    void polymorphicDescribeIsDifferentPerRole() {
        List<Account> all = List.of(
                Accounts.create(1L, "Ann", "a", Role.ADMIN, null),
                Accounts.create(2L, "Ian", "i", Role.ISSUER, "DIT"),
                Accounts.create(3L, "Vera", "v", Role.VERIFIER, null));
        assertTrue(all.get(0).describe().startsWith("Administrator"));
        assertTrue(all.get(1).describe().contains("DIT"));
        assertTrue(all.get(2).describe().startsWith("Verifier"));
    }
}
