package com.digitalcertificate.util;

import java.util.Locale;
import java.util.regex.Pattern;

/** Checks the shape of a certificate ID (CERT-YYYY-NNNNNN) before the database is asked. */
public final class CertificateIdValidator {
    public static final Pattern FORMAT = Pattern.compile("^CERT-\\d{4}-\\d{6,}$");

    private CertificateIdValidator() { }

    public static boolean isValid(String id) {
        return id != null && FORMAT.matcher(id.trim().toUpperCase(Locale.ROOT)).matches();
    }
}
