package com.yiweilai.DreamArchive.DTO;

import lombok.Data;


public class Dream {
    private String id;
    private Integer userId;
    private String time;
    private String place;
    private String content;
    private String emotion;
    private String interpretation;
    private String createdAt;

    public Dream(String id, Integer userId, String time, String place, String content, String emotion, String interpretation) {
        this.id = id;
        this.userId = userId;
        this.time = time;
        this.place = place;
        this.content = content;
        this.emotion = emotion;
        this.interpretation = interpretation;
    }

    public Dream() {
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public void setInterpretation(String interpretation) {
        this.interpretation = interpretation;
    }
}
