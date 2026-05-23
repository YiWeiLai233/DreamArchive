package com.yiweilai.DreamArchive.DTO;

import java.util.List;

public class AdminOverview {
    private Integer totalUsers;
    private Integer adminUsers;
    private Integer totalDreams;
    private Integer todayDreams;
    private List<AdminUserSummary> users;
    private List<AdminDreamSummary> recentDreams;
    private Integer userPage;
    private Integer userPageSize;
    private Integer userResultTotal;
    private Integer userTotalPages;
    private Integer dreamPage;
    private Integer dreamPageSize;
    private Integer dreamResultTotal;
    private Integer dreamTotalPages;

    public Integer getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Integer totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Integer getAdminUsers() {
        return adminUsers;
    }

    public void setAdminUsers(Integer adminUsers) {
        this.adminUsers = adminUsers;
    }

    public Integer getTotalDreams() {
        return totalDreams;
    }

    public void setTotalDreams(Integer totalDreams) {
        this.totalDreams = totalDreams;
    }

    public Integer getTodayDreams() {
        return todayDreams;
    }

    public void setTodayDreams(Integer todayDreams) {
        this.todayDreams = todayDreams;
    }

    public List<AdminUserSummary> getUsers() {
        return users;
    }

    public void setUsers(List<AdminUserSummary> users) {
        this.users = users;
    }

    public List<AdminDreamSummary> getRecentDreams() {
        return recentDreams;
    }

    public void setRecentDreams(List<AdminDreamSummary> recentDreams) {
        this.recentDreams = recentDreams;
    }

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

    public Integer getUserResultTotal() {
        return userResultTotal;
    }

    public void setUserResultTotal(Integer userResultTotal) {
        this.userResultTotal = userResultTotal;
    }

    public Integer getUserTotalPages() {
        return userTotalPages;
    }

    public void setUserTotalPages(Integer userTotalPages) {
        this.userTotalPages = userTotalPages;
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

    public Integer getDreamResultTotal() {
        return dreamResultTotal;
    }

    public void setDreamResultTotal(Integer dreamResultTotal) {
        this.dreamResultTotal = dreamResultTotal;
    }

    public Integer getDreamTotalPages() {
        return dreamTotalPages;
    }

    public void setDreamTotalPages(Integer dreamTotalPages) {
        this.dreamTotalPages = dreamTotalPages;
    }
}
