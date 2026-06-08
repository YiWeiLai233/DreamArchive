package com.yiweilai.DreamArchive.DTO;

import java.time.LocalDateTime;

public class AdminUserSummary {
    private Integer id;
    private String username;
    private String email;
    private String role;
    private String status;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private Integer dreamCount;

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

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getDreamCount() {
        return dreamCount;
    }

    public void setDreamCount(Integer dreamCount) {
        this.dreamCount = dreamCount;
    }
}
