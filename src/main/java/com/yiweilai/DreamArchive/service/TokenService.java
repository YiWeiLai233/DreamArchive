package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class TokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.auth.secret}")
    private String secret;

    @Value("${app.auth.ttl-seconds}")
    private long ttlSeconds;

    public String generateToken(User user) {
        long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = user.getId() + ":" + user.getRole() + ":" + expiresAt;
        return base64Url(payload.getBytes(StandardCharsets.UTF_8)) + "." + sign(payload);
    }

    public AuthenticatedUser parseBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("请先登录");
        }
        return parseToken(authorizationHeader.substring("Bearer ".length()).trim());
    }

    private AuthenticatedUser parseToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("登录状态无效");
        }

        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String expectedSignature = sign(payload);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("登录状态无效");
        }

        String[] payloadParts = payload.split(":");
        if (payloadParts.length != 3) {
            throw new IllegalArgumentException("登录状态无效");
        }

        long expiresAt = Long.parseLong(payloadParts[2]);
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new IllegalArgumentException("登录已过期");
        }

        return new AuthenticatedUser(Integer.parseInt(payloadParts[0]), payloadParts[1]);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return base64Url(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Token 签名失败", e);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static class AuthenticatedUser {
        private final Integer userId;
        private final String role;

        public AuthenticatedUser(Integer userId, String role) {
            this.userId = userId;
            this.role = role;
        }

        public Integer getUserId() {
            return userId;
        }

        public String getRole() {
            return role;
        }
    }
}
