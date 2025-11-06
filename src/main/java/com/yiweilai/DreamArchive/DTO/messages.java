package com.yiweilai.DreamArchive.DTO;

import java.util.List;

public class messages {
    private String role;
    private String content;

    public messages() {
    }

    public messages(String role, String content) {
        this.role = role;
        this.content = content;
    }

    /**
     * 获取
     * @return role
     */
    public String getRole() {
        return role;
    }

    /**
     * 设置
     * @param role
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 获取
     * @return content
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置
     * @param content
     */
    public void setContent(String content) {
        this.content = content;
    }

    public String toString() {
        return "Ai{role = " + role + ", content = " + content + "}";
    }
}
