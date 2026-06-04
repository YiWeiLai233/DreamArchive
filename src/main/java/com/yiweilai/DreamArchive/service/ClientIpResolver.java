package com.yiweilai.DreamArchive.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String forwardedFor = firstForwardedFor(request.getHeader("X-Forwarded-For"));
        if (isUsable(forwardedFor)) {
            return forwardedFor;
        }

        String realIp = cleanIp(request.getHeader("X-Real-IP"));
        if (isUsable(realIp)) {
            return realIp;
        }

        String forwarded = parseForwardedFor(request.getHeader("Forwarded"));
        if (isUsable(forwarded)) {
            return forwarded;
        }

        String remoteAddr = cleanIp(request.getRemoteAddr());
        return isUsable(remoteAddr) ? remoteAddr : "unknown";
    }

    private String firstForwardedFor(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (String part : value.split(",")) {
            String candidate = cleanIp(part);
            if (isUsable(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String parseForwardedFor(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (String part : value.split(";")) {
            String trimmed = part.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("for=")) {
                return cleanIp(trimmed.substring(4));
            }
        }
        return null;
    }

    private String cleanIp(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 1) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.startsWith("[") && cleaned.contains("]")) {
            cleaned = cleaned.substring(1, cleaned.indexOf(']'));
        }
        return cleaned;
    }

    private boolean isUsable(String value) {
        return value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value.trim());
    }
}
