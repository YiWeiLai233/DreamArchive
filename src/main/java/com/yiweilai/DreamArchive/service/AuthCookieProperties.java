package com.yiweilai.DreamArchive.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthCookieProperties {
    private String cookieName = "dream_archive_token";
    private Boolean cookieSecure = false;
    private String cookieSameSite = "Lax";
    private Long ttlSeconds = 86400L;
    private String csrfCookieName = "XSRF-TOKEN";
    private String csrfHeaderName = "X-XSRF-TOKEN";
    private Boolean csrfEnabled = true;

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        if (cookieName != null && !cookieName.isBlank()) {
            this.cookieName = cookieName;
        }
    }

    public Boolean getCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(Boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String getCookieSameSite() {
        return cookieSameSite;
    }

    public void setCookieSameSite(String cookieSameSite) {
        if (cookieSameSite != null && !cookieSameSite.isBlank()) {
            this.cookieSameSite = cookieSameSite;
        }
    }

    public Long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Long ttlSeconds) {
        if (ttlSeconds != null && ttlSeconds > 0) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public String getCsrfCookieName() {
        return csrfCookieName;
    }

    public void setCsrfCookieName(String csrfCookieName) {
        if (csrfCookieName != null && !csrfCookieName.isBlank()) {
            this.csrfCookieName = csrfCookieName;
        }
    }

    public String getCsrfHeaderName() {
        return csrfHeaderName;
    }

    public void setCsrfHeaderName(String csrfHeaderName) {
        if (csrfHeaderName != null && !csrfHeaderName.isBlank()) {
            this.csrfHeaderName = csrfHeaderName;
        }
    }

    public Boolean getCsrfEnabled() {
        return csrfEnabled;
    }

    public void setCsrfEnabled(Boolean csrfEnabled) {
        this.csrfEnabled = csrfEnabled;
    }
}
