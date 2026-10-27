package com.digitalcertificate.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternsTest {
    @Test
    void acceptsPasswordWithinBcryptByteLimit() {
        assertTrue(Patterns.isValidPassword("correctHorse42"));
        assertTrue(Patterns.isValidPassword("ééééééé1A"));
    }

    @Test
    void rejectsPasswordThatExceedsBcryptByteLimit() {
        assertFalse(Patterns.isValidPassword("é".repeat(36) + "1A"));
    }

    @Test
    void rejectsNullOrWeakPasswords() {
        assertFalse(Patterns.isValidPassword(null));
        assertFalse(Patterns.isValidPassword("short1"));
        assertFalse(Patterns.isValidPassword("long-but-no-number"));
    }
}
