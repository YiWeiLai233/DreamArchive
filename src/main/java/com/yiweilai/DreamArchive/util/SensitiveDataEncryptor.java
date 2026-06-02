package com.yiweilai.DreamArchive.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SensitiveDataEncryptor {

    private static final int BCRYPT_STRENGTH = 12;
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("\\A\\$2[aby]?\\$\\d{2}\\$[./0-9A-Za-z]{53}");

    private final BCryptPasswordEncoder bcryptPasswordEncoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

    public String encrypt(String rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("rawValue cannot be null");
        }
        return bcryptPasswordEncoder.encode(rawValue);
    }

    public boolean matches(String rawValue, String encryptedValue) {
        if (rawValue == null || encryptedValue == null || encryptedValue.isBlank()) {
            return false;
        }
        if (!BCRYPT_PATTERN.matcher(encryptedValue).matches()) {
            return false;
        }
        try {
            return bcryptPasswordEncoder.matches(rawValue, encryptedValue);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
