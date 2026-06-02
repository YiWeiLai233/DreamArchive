package com.yiweilai.DreamArchive.service;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationCodeServiceTest {

    @Test
    void verificationCodesUseSecureRandomSource() {
        assertTrue(Arrays.stream(VerificationCodeService.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(SecureRandom.class)));
        assertFalse(Arrays.stream(VerificationCodeService.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(Random.class)));
    }
}
