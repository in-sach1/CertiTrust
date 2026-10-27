package com.digitalcertificate.util;

import java.nio.charset.StandardCharsets;

public final class Patterns {
    private Patterns() { }

    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    public static final String PASSWORD_MESSAGE =
            "Password must be 8-72 characters (at most 72 UTF-8 bytes) with at least one letter and one number";

    public static boolean isValidPassword(String password) {
        return password != null
                && password.matches(PASSWORD_REGEX)
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
