package com.yiweilai.DreamArchive.DTO;

import java.time.LocalDateTime;

public class LoginResponse {
    private Integer id;
    private String username;
    private String email;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private String token;
    private boolean needsSetup;

    public LoginResponse() {
    }

    public LoginResponse(User user, String token) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.createdAt = user.getCreatedAt();
        this.token = token;
        this.needsSetup = false;
    }

    public LoginResponse(User user, String token, boolean needsSetup) {
        this(user, token);
        this.needsSetup = needsSetup;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isNeedsSetup() {
        return needsSetup;
    }

    public void setNeedsSetup(boolean needsSetup) {
        this.needsSetup = needsSetup;
    }
}
