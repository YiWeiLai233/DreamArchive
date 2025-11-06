package com.yiweilai.DreamArchive.DTO;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;


public class DreamRequest {
    private String DreamText;

    public String getDreamText() {
        return DreamText;
    }

    public void setDreamText(String dreamText) {
        DreamText = dreamText;
    }

    public DreamRequest(String dreamText) {
        DreamText = dreamText;
    }

    public DreamRequest() {
    }

}
