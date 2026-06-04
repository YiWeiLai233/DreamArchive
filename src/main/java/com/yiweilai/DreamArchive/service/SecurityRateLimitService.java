package com.yiweilai.DreamArchive.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class SecurityRateLimitService {
    public static final String RATE_LIMIT_MESSAGE = "操作过于频繁，请稍后再试";

    private static final Logger log = LoggerFactory.getLogger(SecurityRateLimitService.class);
    private static final String PREFIX = "security:rate:";

    private final StringRedisTemplate redisTemplate;
    private final SecurityRateLimitProperties properties;

    public SecurityRateLimitService(StringRedisTemplate redisTemplate, SecurityRateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public boolean isPasswordLoginBlocked(String ip, String identifier) {
        if (!enabled()) {
            return false;
        }
        Duration window = Duration.ofSeconds(properties.getLoginFailureWindowSeconds());
        return anyBlocked(List.of(
                bucket("login-fail:ip", properties.getLoginFailureIpMax(), window, ip),
                bucket("login-fail:identifier", properties.getLoginFailureIdentifierMax(), window, identifier),
                bucket("login-fail:ip-identifier", properties.getLoginFailureIpIdentifierMax(), window, ip, identifier)
        ));
    }

    public void recordPasswordLoginFailure(String ip, String identifier) {
        if (!enabled()) {
            return;
        }
        Duration window = Duration.ofSeconds(properties.getLoginFailureWindowSeconds());
        incrementAll(List.of(
                bucket("login-fail:ip", properties.getLoginFailureIpMax(), window, ip),
                bucket("login-fail:identifier", properties.getLoginFailureIdentifierMax(), window, identifier),
                bucket("login-fail:ip-identifier", properties.getLoginFailureIpIdentifierMax(), window, ip, identifier)
        ));
    }

    public void clearPasswordLoginFailures(String ip, String identifier) {
        deleteAll(List.of(
                bucket("login-fail:identifier", properties.getLoginFailureIdentifierMax(),
                        Duration.ofSeconds(properties.getLoginFailureWindowSeconds()), identifier),
                bucket("login-fail:ip-identifier", properties.getLoginFailureIpIdentifierMax(),
                        Duration.ofSeconds(properties.getLoginFailureWindowSeconds()), ip, identifier)
        ));
    }

    public boolean consumeVerificationSend(String scene, String ip) {
        return consume(bucket("verify-send:" + scene + ":ip",
                properties.getVerificationSendIpMax(),
                Duration.ofSeconds(properties.getVerificationSendIpWindowSeconds()),
                ip));
    }

    public boolean isVerificationCheckBlocked(String scene, String ip, String identifier) {
        if (!enabled()) {
            return false;
        }
        Duration window = Duration.ofSeconds(properties.getVerificationFailureWindowSeconds());
        return anyBlocked(List.of(
                bucket("verify-fail:" + scene + ":ip", properties.getVerificationFailureIpMax(), window, ip),
                bucket("verify-fail:" + scene + ":identifier", properties.getVerificationFailureIdentifierMax(), window, identifier),
                bucket("verify-fail:" + scene + ":ip-identifier", properties.getVerificationFailureIpIdentifierMax(), window, ip, identifier)
        ));
    }

    public void recordVerificationCheckFailure(String scene, String ip, String identifier) {
        if (!enabled()) {
            return;
        }
        Duration window = Duration.ofSeconds(properties.getVerificationFailureWindowSeconds());
        incrementAll(List.of(
                bucket("verify-fail:" + scene + ":ip", properties.getVerificationFailureIpMax(), window, ip),
                bucket("verify-fail:" + scene + ":identifier", properties.getVerificationFailureIdentifierMax(), window, identifier),
                bucket("verify-fail:" + scene + ":ip-identifier", properties.getVerificationFailureIpIdentifierMax(), window, ip, identifier)
        ));
    }

    public void clearVerificationCheckFailures(String scene, String ip, String identifier) {
        Duration window = Duration.ofSeconds(properties.getVerificationFailureWindowSeconds());
        deleteAll(List.of(
                bucket("verify-fail:" + scene + ":identifier", properties.getVerificationFailureIdentifierMax(), window, identifier),
                bucket("verify-fail:" + scene + ":ip-identifier", properties.getVerificationFailureIpIdentifierMax(), window, ip, identifier)
        ));
    }

    public boolean consumeGuestAnalyzeIp(String ip) {
        return consume(bucket("guest-analyze:ip",
                properties.getGuestAnalyzeIpMax(),
                Duration.ofSeconds(properties.getGuestAnalyzeIpWindowSeconds()),
                ip));
    }

    private boolean anyBlocked(List<Bucket> buckets) {
        for (Bucket bucket : buckets) {
            if (isBlocked(bucket)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlocked(Bucket bucket) {
        if (!enabled() || bucket.max <= 0) {
            return false;
        }
        try {
            String value = redisTemplate.opsForValue().get(bucket.key);
            if (value == null) {
                return false;
            }
            return Long.parseLong(value) >= bucket.max;
        } catch (RuntimeException e) {
            log.warn("Rate limit read failed for {}", bucket.category, e);
            return false;
        }
    }

    private boolean consume(Bucket bucket) {
        if (!enabled() || bucket.max <= 0) {
            return false;
        }
        try {
            Long count = redisTemplate.opsForValue().increment(bucket.key, 1L);
            if (Long.valueOf(1L).equals(count)) {
                redisTemplate.expire(bucket.key, bucket.window);
            }
            return count != null && count > bucket.max;
        } catch (RuntimeException e) {
            log.warn("Rate limit increment failed for {}", bucket.category, e);
            return false;
        }
    }

    private void incrementAll(List<Bucket> buckets) {
        for (Bucket bucket : buckets) {
            consume(bucket);
        }
    }

    private void deleteAll(List<Bucket> buckets) {
        if (!enabled()) {
            return;
        }
        for (Bucket bucket : buckets) {
            try {
                redisTemplate.delete(bucket.key);
            } catch (RuntimeException e) {
                log.warn("Rate limit delete failed for {}", bucket.category, e);
            }
        }
    }

    private Bucket bucket(String category, int max, Duration window, String... values) {
        StringBuilder key = new StringBuilder(PREFIX).append(category);
        for (String value : values) {
            key.append(':').append(hash(normalize(value)));
        }
        return new Bucket(category, key.toString(), max, window);
    }

    private boolean enabled() {
        return Boolean.TRUE.equals(properties.getEnabled());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private record Bucket(String category, String key, int max, Duration window) {
    }
}
