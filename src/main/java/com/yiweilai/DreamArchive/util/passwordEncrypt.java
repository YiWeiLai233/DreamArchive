package com.yiweilai.DreamArchive.util;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class passwordEncrypt {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 使用 SHA-256 + 随机盐值加密密码
     * 存储格式: base64(salt):base64(sha256(salt+password))
     */
    public String encrypt(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = sha256(salt, password);
        return saltBase64 + ":" + hashBase64;
    }

    /**
     * 验证明文密码是否与存储的加密密码匹配
     * encodedPassword 格式: base64(salt):base64(sha256(salt+password))
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || !encodedPassword.contains(":")) {
            return false;
        }
        String[] parts = encodedPassword.split(":");
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        String hashBase64 = sha256(salt, rawPassword);
        return hashBase64.equals(parts[1]);
    }

    private String sha256(byte[] salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 算法不可用", e);
        }
    }
}
