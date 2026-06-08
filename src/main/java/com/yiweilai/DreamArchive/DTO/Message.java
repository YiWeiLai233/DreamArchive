package com.yiweilai.DreamArchive.DTO;

import java.util.List;
import java.util.Map;

public class Message {
    private String role;
    private Object content;

    public Message() {
    }

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public Message(String role, List<Map<String, Object>> contentParts) {
        this.role = role;
        this.content = contentParts;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String toString() {
        return "Ai{role = " + role + ", content = " + content + "}";
    }
}
