package com.digitalcertificate.domain;

/** Things an account may be allowed to do. Each account type grants its own set. */
public enum Permission {
    MANAGE_USERS,
    MANAGE_INSTITUTIONS,
    ISSUE_CERTIFICATES,
    REVOKE_CERTIFICATES,
    VERIFY_CERTIFICATES,
    SEARCH_CERTIFICATES,
    VIEW_HISTORY,
    VIEW_AUDIT_AND_REPORTS
}
