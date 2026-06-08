package com.yiweilai.DreamArchive.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class AuthCookieService {
    private final AuthCookieProperties properties;

    public AuthCookieService(AuthCookieProperties properties) {
        this.properties = properties;
    }

    public void addAuthCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(properties.getCookieName(), token, true, properties.getTtlSeconds()).toString());
    }

    public void clearAuthCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(properties.getCookieName(), "", true, 0).toString());
    }

    public void addCsrfCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(properties.getCsrfCookieName(), token, false, properties.getTtlSeconds()).toString());
    }

    public void clearCsrfCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(properties.getCsrfCookieName(), "", false, 0).toString());
    }

    public Optional<String> readTokenFromCookie(HttpServletRequest request) {
        return readCookie(request, properties.getCookieName());
    }

    public Optional<String> readCsrfFromCookie(HttpServletRequest request) {
        return readCookie(request, properties.getCsrfCookieName());
    }

    public boolean hasAuthCookie(HttpServletRequest request) {
        return readTokenFromCookie(request).isPresent();
    }

    private ResponseCookie buildCookie(String name, String value, boolean httpOnly, long maxAgeSeconds) {
        return ResponseCookie.from(name, value == null ? "" : value)
                .path("/")
                .httpOnly(httpOnly)
                .secure(Boolean.TRUE.equals(properties.getCookieSecure()))
                .sameSite(properties.getCookieSameSite())
                .maxAge(Duration.ofSeconds(Math.max(0, maxAgeSeconds)))
                .build();
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        if (request == null || request.getCookies() == null || name == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
