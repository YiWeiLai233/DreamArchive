package com.yiweilai.DreamArchive.DTO;

import java.time.LocalDateTime;

public class DreamContent {
    private String id;
    private Integer userId;
    private String title;
    private String content;
    private String emotion;
    private String place;
    private String time;
    private String interpretation;
    private LocalDateTime createdAt;

    public DreamContent() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; }

    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getInterpretation() { return interpretation; }
    public void setInterpretation(String interpretation) { this.interpretation = interpretation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
