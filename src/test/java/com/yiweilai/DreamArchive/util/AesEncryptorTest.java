package com.yiweilai.DreamArchive.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesEncryptorTest {

    private static final String TEST_KEY = "TestAesKey12345678901234567890ab"; // 32 chars
    private AesEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new AesEncryptor(TEST_KEY);
    }

    @Test
    void encryptReturnsNonNullBase64String() {
        String encrypted = encryptor.encrypt("test-api-key");
        assertNotNull(encrypted);
        assertFalse(encrypted.isBlank());
        // Base64 字符集验证
        assertTrue(encrypted.matches("[A-Za-z0-9+/=]+"));
    }

    @Test
    void decryptReturnsOriginalPlainText() {
        String plainText = "sk-abcdefghijklmnopqrstuvwxyz123456";
        String encrypted = encryptor.encrypt(plainText);
        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void encryptProducesDifferentCiphertextEachTime() {
        String plainText = "same-input";
        String first = encryptor.encrypt(plainText);
        String second = encryptor.encrypt(plainText);
        // GCM 使用随机 IV，每次密文不同
        assertNotEquals(first, second);
        // 但解密后相同
        assertEquals(plainText, encryptor.decrypt(first));
        assertEquals(plainText, encryptor.decrypt(second));
    }

    @Test
    void encryptHandlesSpecialCharacters() {
        String special = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~";
        String encrypted = encryptor.encrypt(special);
        assertEquals(special, encryptor.decrypt(encrypted));
    }

    @Test
    void encryptHandlesChineseCharacters() {
        String chinese = "这是一个测试apiKey";
        String encrypted = encryptor.encrypt(chinese);
        assertEquals(chinese, encryptor.decrypt(encrypted));
    }

    @Test
    void encryptHandlesEmptyString() {
        String encrypted = encryptor.encrypt("");
        assertEquals("", encryptor.decrypt(encrypted));
    }

    @Test
    void encryptReturnsNullForNullInput() {
        assertNull(encryptor.encrypt(null));
    }

    @Test
    void decryptThrowsOnInvalidCipherText() {
        assertThrows(IllegalArgumentException.class, () -> encryptor.decrypt("not-valid-base64!!!"));
    }

    @Test
    void decryptThrowsOnTooShortCipherText() {
        // Base64 解码后长度 < IV_LENGTH(12) + 16
        String tooShort = java.util.Base64.getEncoder().encodeToString(new byte[10]);
        assertThrows(IllegalArgumentException.class, () -> encryptor.decrypt(tooShort));
    }

    @Test
    void isEncryptedReturnsTrueForValidCipherText() {
        String encrypted = encryptor.encrypt("test");
        assertTrue(encryptor.isEncrypted(encrypted));
    }

    @Test
    void isEncryptedReturnsFalseForPlainText() {
        assertFalse(encryptor.isEncrypted("plain-api-key"));
        assertFalse(encryptor.isEncrypted("sk-12345"));
    }

    @Test
    void isEncryptedReturnsFalseForNullOrBlank() {
        assertFalse(encryptor.isEncrypted(null));
        assertFalse(encryptor.isEncrypted(""));
        assertFalse(encryptor.isEncrypted("   "));
    }

    @Test
    void constructorRejectsWrongKeyLength() {
        assertThrows(IllegalArgumentException.class, () -> new AesEncryptor("short"));
        assertThrows(IllegalArgumentException.class, () -> new AesEncryptor("1234567890123456789012345678901")); // 31 chars
        assertThrows(IllegalArgumentException.class, () -> new AesEncryptor("123456789012345678901234567890123")); // 33 chars
    }

    @Test
    void constructorAcceptsExact32Chars() {
        assertDoesNotThrow(() -> new AesEncryptor("12345678901234567890123456789012"));
    }

    @Test
    void decryptWithWrongKeyFails() {
        String encrypted = encryptor.encrypt("secret-data");
        AesEncryptor differentKeyEncryptor = new AesEncryptor("DifferentKey12345678901234567890");
        assertThrows(IllegalStateException.class, () -> differentKeyEncryptor.decrypt(encrypted));
    }

    @Test
    void roundTripWithLongApiKey() {
        String longKey = "sk-proj-" + "a".repeat(200);
        String encrypted = encryptor.encrypt(longKey);
        assertEquals(longKey, encryptor.decrypt(encrypted));
    }
}
