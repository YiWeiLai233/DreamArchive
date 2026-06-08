package com.yiweilai.DreamArchive.DTO;

import java.time.LocalDateTime;

public class DreamPlaceStats {
    private Integer id;
    private Integer userId;
    private String place;
    private Integer dreamCount;
    private LocalDateTime updatedAt;

    public DreamPlaceStats() {}

    public DreamPlaceStats(Integer userId, String place) {
        this.userId = userId;
        this.place = place;
        this.dreamCount = 0;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public Integer getDreamCount() {
        return dreamCount;
    }

    public void setDreamCount(Integer dreamCount) {
        this.dreamCount = dreamCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
