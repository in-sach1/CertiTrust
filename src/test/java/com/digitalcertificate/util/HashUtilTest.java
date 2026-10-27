package com.digitalcertificate.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class HashUtilTest {

    @Test
    void sha256_matchesKnownTestVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", HashUtil.sha256Hex("abc"));
    }

    @Test
    void sha256_isAlways64HexCharacters() {
        assertEquals(64, HashUtil.sha256Hex("any certificate text").length());
    }

    @Test
    void sha256_changesWhenInputChanges() {
        assertNotEquals(HashUtil.sha256Hex("Jane Doe"), HashUtil.sha256Hex("Jane Doe "));
    }
}
