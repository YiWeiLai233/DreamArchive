package com.yiweilai.DreamArchive.DTO;

import lombok.Data;


public class Dream {
    private String id;
    private String time;
    private String place;
    private String content;
    private String emotion;
    private String Interpretation;

    public Dream(String id, String time, String place, String content, String emotion, String interpretation) {
        this.id = id;
        this.time = time;
        this.place = place;
        this.content = content;
        this.emotion = emotion;
        Interpretation = interpretation;
    }

    public Dream() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        return Interpretation;
    }

    public void setInterpretation(String interpretation) {
        Interpretation = interpretation;
    }
}
