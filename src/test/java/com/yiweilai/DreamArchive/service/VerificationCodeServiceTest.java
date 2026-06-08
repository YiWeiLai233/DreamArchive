package com.yiweilai.DreamArchive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationCodeServiceTest {
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> operations;
    private JavaMailSender mailSender;
    private SecurityRateLimitService rateLimitService;
    private VerificationCodeService service;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        operations = mock(ValueOperations.class);
        mailSender = mock(JavaMailSender.class);
        rateLimitService = mock(SecurityRateLimitService.class);
        service = new VerificationCodeService();
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(service, "mailSender", mailSender);
        ReflectionTestUtils.setField(service, "rateLimitService", rateLimitService);
        when(redisTemplate.opsForValue()).thenReturn(operations);
    }

    @Test
    void verificationCodesUseSecureRandomSource() {
        assertTrue(Arrays.stream(VerificationCodeService.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(SecureRandom.class)));
        assertFalse(Arrays.stream(VerificationCodeService.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(Random.class)));
    }

    @Test
    void failedVerificationRecordsSecurityFailure() {
        when(operations.get("code:login:alice@example.com")).thenReturn("123456");

        assertThat(service.verifyCode("login", "alice@example.com", "000000", "203.0.113.10")).isFalse();

        verify(rateLimitService).recordVerificationCheckFailure("login", "203.0.113.10", "alice@example.com");
    }

    @Test
    void blockedVerificationDoesNotReadStoredCode() {
        when(rateLimitService.isVerificationCheckBlocked("login", "203.0.113.10", "alice@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.verifyCode("login", "alice@example.com", "000000", "203.0.113.10"))
                .isInstanceOf(RateLimitExceededException.class);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void emailSenderUsesConfiguredFromAddress() {
        ReflectionTestUtils.setField(service, "mailFrom", "no-reply@example.com");
        when(redisTemplate.hasKey("rate:login:alice@example.com")).thenReturn(false);

        service.sendCode("login", "alice@example.com", "alice@example.com");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getFrom()).isEqualTo("no-reply@example.com");
        verify(operations).set(eq("rate:login:alice@example.com"), eq("1"), eq(60L), eq(java.util.concurrent.TimeUnit.SECONDS));
    }
}
