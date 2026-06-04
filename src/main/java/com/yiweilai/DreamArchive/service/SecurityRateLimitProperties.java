package com.yiweilai.DreamArchive.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.rate-limit")
public class SecurityRateLimitProperties {
    private Boolean enabled = true;
    private int loginFailureIpMax = 20;
    private int loginFailureIdentifierMax = 5;
    private int loginFailureIpIdentifierMax = 5;
    private Long loginFailureWindowSeconds = 900L;
    private int verificationSendIpMax = 10;
    private Long verificationSendIpWindowSeconds = 600L;
    private int verificationFailureIpMax = 30;
    private int verificationFailureIdentifierMax = 5;
    private int verificationFailureIpIdentifierMax = 5;
    private Long verificationFailureWindowSeconds = 300L;
    private int guestAnalyzeIpMax = 20;
    private Long guestAnalyzeIpWindowSeconds = 86400L;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public int getLoginFailureIpMax() {
        return loginFailureIpMax;
    }

    public void setLoginFailureIpMax(int loginFailureIpMax) {
        this.loginFailureIpMax = Math.max(0, loginFailureIpMax);
    }

    public int getLoginFailureIdentifierMax() {
        return loginFailureIdentifierMax;
    }

    public void setLoginFailureIdentifierMax(int loginFailureIdentifierMax) {
        this.loginFailureIdentifierMax = Math.max(0, loginFailureIdentifierMax);
    }

    public int getLoginFailureIpIdentifierMax() {
        return loginFailureIpIdentifierMax;
    }

    public void setLoginFailureIpIdentifierMax(int loginFailureIpIdentifierMax) {
        this.loginFailureIpIdentifierMax = Math.max(0, loginFailureIpIdentifierMax);
    }

    public Long getLoginFailureWindowSeconds() {
        return loginFailureWindowSeconds;
    }

    public void setLoginFailureWindowSeconds(Long loginFailureWindowSeconds) {
        if (loginFailureWindowSeconds != null && loginFailureWindowSeconds > 0) {
            this.loginFailureWindowSeconds = loginFailureWindowSeconds;
        }
    }

    public int getVerificationSendIpMax() {
        return verificationSendIpMax;
    }

    public void setVerificationSendIpMax(int verificationSendIpMax) {
        this.verificationSendIpMax = Math.max(0, verificationSendIpMax);
    }

    public Long getVerificationSendIpWindowSeconds() {
        return verificationSendIpWindowSeconds;
    }

    public void setVerificationSendIpWindowSeconds(Long verificationSendIpWindowSeconds) {
        if (verificationSendIpWindowSeconds != null && verificationSendIpWindowSeconds > 0) {
            this.verificationSendIpWindowSeconds = verificationSendIpWindowSeconds;
        }
    }

    public int getVerificationFailureIpMax() {
        return verificationFailureIpMax;
    }

    public void setVerificationFailureIpMax(int verificationFailureIpMax) {
        this.verificationFailureIpMax = Math.max(0, verificationFailureIpMax);
    }

    public int getVerificationFailureIdentifierMax() {
        return verificationFailureIdentifierMax;
    }

    public void setVerificationFailureIdentifierMax(int verificationFailureIdentifierMax) {
        this.verificationFailureIdentifierMax = Math.max(0, verificationFailureIdentifierMax);
    }

    public int getVerificationFailureIpIdentifierMax() {
        return verificationFailureIpIdentifierMax;
    }

    public void setVerificationFailureIpIdentifierMax(int verificationFailureIpIdentifierMax) {
        this.verificationFailureIpIdentifierMax = Math.max(0, verificationFailureIpIdentifierMax);
    }

    public Long getVerificationFailureWindowSeconds() {
        return verificationFailureWindowSeconds;
    }

    public void setVerificationFailureWindowSeconds(Long verificationFailureWindowSeconds) {
        if (verificationFailureWindowSeconds != null && verificationFailureWindowSeconds > 0) {
            this.verificationFailureWindowSeconds = verificationFailureWindowSeconds;
        }
    }

    public int getGuestAnalyzeIpMax() {
        return guestAnalyzeIpMax;
    }

    public void setGuestAnalyzeIpMax(int guestAnalyzeIpMax) {
        this.guestAnalyzeIpMax = Math.max(0, guestAnalyzeIpMax);
    }

    public Long getGuestAnalyzeIpWindowSeconds() {
        return guestAnalyzeIpWindowSeconds;
    }

    public void setGuestAnalyzeIpWindowSeconds(Long guestAnalyzeIpWindowSeconds) {
        if (guestAnalyzeIpWindowSeconds != null && guestAnalyzeIpWindowSeconds > 0) {
            this.guestAnalyzeIpWindowSeconds = guestAnalyzeIpWindowSeconds;
        }
    }
}
