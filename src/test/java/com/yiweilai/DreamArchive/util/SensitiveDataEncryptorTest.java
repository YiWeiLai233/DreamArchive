package com.yiweilai.DreamArchive.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveDataEncryptorTest {

    private final SensitiveDataEncryptor encryptor = new SensitiveDataEncryptor();

    @Test
    void encryptUsesBCryptAndMatchesOriginalValue() {
        String encrypted = encryptor.encrypt("my-secret-password");

        assertTrue(encrypted.startsWith("$2"));
        assertTrue(encryptor.matches("my-secret-password", encrypted));
    }

    @Test
    void encryptUsesDifferentSaltForSameValue() {
        String first = encryptor.encrypt("same-secret");
        String second = encryptor.encrypt("same-secret");

        assertNotEquals(first, second);
        assertTrue(encryptor.matches("same-secret", first));
        assertTrue(encryptor.matches("same-secret", second));
    }

    @Test
    void matchesRejectsWrongOrInvalidValues() {
        String encrypted = encryptor.encrypt("correct-secret");

        assertFalse(encryptor.matches("wrong-secret", encrypted));
        assertFalse(encryptor.matches(null, encrypted));
        assertFalse(encryptor.matches("correct-secret", null));
        assertFalse(encryptor.matches("correct-secret", "not-a-bcrypt-hash"));
    }
}
