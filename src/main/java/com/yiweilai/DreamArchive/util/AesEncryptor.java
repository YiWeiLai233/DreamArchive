package com.yiweilai.DreamArchive.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM 可逆加密工具，用于加密存储 apiKey 等敏感信息
 */
@Component
public class AesEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKeySpec secretKey;

    public AesEncryptor(@Value("${app.encryption.aes-key}") String aesKey) {
        if (aesKey == null || aesKey.length() != 32) {
            throw new IllegalArgumentException("app.encryption.aes-key must be exactly 32 characters");
        }
        this.secretKey = new SecretKeySpec(aesKey.getBytes(StandardCharsets.UTF_8), "AES");
    }

    /**
     * 加密明文，返回 Base64 编码的密文（包含 IV）
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + 密文 拼接后 Base64
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("AES encryption failed", e);
        }
    }

    /**
     * 解密 Base64 密文，返回明文
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return cipherText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            if (combined.length < IV_LENGTH) {
                throw new IllegalArgumentException("Invalid cipher text");
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("AES decryption failed", e);
        }
    }

    /**
     * 判断是否已经是加密格式（Base64 且长度合理）
     */
    public boolean isEncrypted(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            // GCM 密文最小长度 = IV(12) + 最小密文(16) = 28 字节
            return decoded.length >= IV_LENGTH + 16;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
