package com.yiweilai.DreamArchive.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityRateLimitServiceTest {

    @Test
    void consumesGuestAnalyzeIpLimitAndSetsWindowOnFirstHit() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.increment(anyString(), eq(1L))).thenReturn(1L);
        SecurityRateLimitProperties properties = new SecurityRateLimitProperties();
        properties.setGuestAnalyzeIpMax(2);
        properties.setGuestAnalyzeIpWindowSeconds(60L);
        SecurityRateLimitService service = new SecurityRateLimitService(redisTemplate, properties);

        assertThat(service.consumeGuestAnalyzeIp("203.0.113.10")).isFalse();

        verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(60)));
    }

    @Test
    void reportsLimitExceededWhenCounterPassesMax() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.increment(anyString(), eq(1L))).thenReturn(3L);
        SecurityRateLimitProperties properties = new SecurityRateLimitProperties();
        properties.setVerificationSendIpMax(2);
        SecurityRateLimitService service = new SecurityRateLimitService(redisTemplate, properties);

        assertThat(service.consumeVerificationSend("login", "203.0.113.10")).isTrue();

        verify(redisTemplate, never()).expire(anyString(), eq(Duration.ofSeconds(properties.getVerificationSendIpWindowSeconds())));
    }

    @Test
    void blocksPasswordLoginWhenAnyFailureDimensionIsOverLimit() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.get(anyString())).thenReturn("999");
        SecurityRateLimitProperties properties = new SecurityRateLimitProperties();
        properties.setLoginFailureIpMax(20);
        properties.setLoginFailureIdentifierMax(5);
        properties.setLoginFailureIpIdentifierMax(5);
        SecurityRateLimitService service = new SecurityRateLimitService(redisTemplate, properties);

        assertThat(service.isPasswordLoginBlocked("203.0.113.10", "alice@example.com")).isTrue();
    }
}
