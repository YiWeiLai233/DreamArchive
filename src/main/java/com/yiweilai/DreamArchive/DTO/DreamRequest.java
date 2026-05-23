package com.yiweilai.DreamArchive.DTO;

public class DreamRequest {
    private String dreamText;

    public String getDreamText() {
        return dreamText;
    }

    public void setDreamText(String dreamText) {
        this.dreamText = dreamText;
    }

    public DreamRequest(String dreamText) {
        this.dreamText = dreamText;
    }

    public DreamRequest() {
    }

}
