package com.yiweilai.DreamArchive.DTO;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DreamStatsEntity {
    private Integer id;
    private Integer userId;
    private LocalDate statDate;
    private Integer totalDreams;
    private Integer happyCount;
    private Integer sadCount;
    private Integer scaredCount;
    private Integer angryCount;
    private Integer peacefulCount;
    private Integer otherEmotionCount;
    private LocalDateTime updatedAt;

    public DreamStatsEntity() {}

    public DreamStatsEntity(Integer userId, LocalDate statDate) {
        this.userId = userId;
        this.statDate = statDate;
        this.totalDreams = 0;
        this.happyCount = 0;
        this.sadCount = 0;
        this.scaredCount = 0;
        this.angryCount = 0;
        this.peacefulCount = 0;
        this.otherEmotionCount = 0;
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

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public Integer getTotalDreams() {
        return totalDreams;
    }

    public void setTotalDreams(Integer totalDreams) {
        this.totalDreams = totalDreams;
    }

    public Integer getHappyCount() {
        return happyCount;
    }

    public void setHappyCount(Integer happyCount) {
        this.happyCount = happyCount;
    }

    public Integer getSadCount() {
        return sadCount;
    }

    public void setSadCount(Integer sadCount) {
        this.sadCount = sadCount;
    }

    public Integer getScaredCount() {
        return scaredCount;
    }

    public void setScaredCount(Integer scaredCount) {
        this.scaredCount = scaredCount;
    }

    public Integer getAngryCount() {
        return angryCount;
    }

    public void setAngryCount(Integer angryCount) {
        this.angryCount = angryCount;
    }

    public Integer getPeacefulCount() {
        return peacefulCount;
    }

    public void setPeacefulCount(Integer peacefulCount) {
        this.peacefulCount = peacefulCount;
    }

    public Integer getOtherEmotionCount() {
        return otherEmotionCount;
    }

    public void setOtherEmotionCount(Integer otherEmotionCount) {
        this.otherEmotionCount = otherEmotionCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
