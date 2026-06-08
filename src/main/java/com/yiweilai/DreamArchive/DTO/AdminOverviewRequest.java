package com.yiweilai.DreamArchive.DTO;

public class AdminOverviewRequest {
    private Integer userPage;
    private Integer userPageSize;
    private String userKeyword;
    private Integer dreamPage;
    private Integer dreamPageSize;
    private String dreamKeyword;

    public Integer getUserPage() {
        return userPage;
    }

    public void setUserPage(Integer userPage) {
        this.userPage = userPage;
    }

    public Integer getUserPageSize() {
        return userPageSize;
    }

    public void setUserPageSize(Integer userPageSize) {
        this.userPageSize = userPageSize;
    }

    public String getUserKeyword() {
        return userKeyword;
    }

    public void setUserKeyword(String userKeyword) {
        this.userKeyword = userKeyword;
    }

    public Integer getDreamPage() {
        return dreamPage;
    }

    public void setDreamPage(Integer dreamPage) {
        this.dreamPage = dreamPage;
    }

    public Integer getDreamPageSize() {
        return dreamPageSize;
    }

    public void setDreamPageSize(Integer dreamPageSize) {
        this.dreamPageSize = dreamPageSize;
    }

    public String getDreamKeyword() {
        return dreamKeyword;
    }

    public void setDreamKeyword(String dreamKeyword) {
        this.dreamKeyword = dreamKeyword;
    }
}
