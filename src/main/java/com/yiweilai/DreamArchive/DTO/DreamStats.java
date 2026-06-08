package com.yiweilai.DreamArchive.DTO;

import java.util.List;
import java.util.Map;

public class DreamStats {
    private Integer userId;
    private Integer totalDreams;
    private List<Map<String, Object>> emotionDistribution;
    private List<Map<String, Object>> placeDistribution;
    private List<Map<String, Object>> recentTrend;

    public DreamStats() {}

    public DreamStats(Integer userId, Integer totalDreams,
                      List<Map<String, Object>> emotionDistribution,
                      List<Map<String, Object>> placeDistribution,
                      List<Map<String, Object>> recentTrend) {
        this.userId = userId;
        this.totalDreams = totalDreams;
        this.emotionDistribution = emotionDistribution;
        this.placeDistribution = placeDistribution;
        this.recentTrend = recentTrend;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getTotalDreams() {
        return totalDreams;
    }

    public void setTotalDreams(Integer totalDreams) {
        this.totalDreams = totalDreams;
    }

    public List<Map<String, Object>> getEmotionDistribution() {
        return emotionDistribution;
    }

    public void setEmotionDistribution(List<Map<String, Object>> emotionDistribution) {
        this.emotionDistribution = emotionDistribution;
    }

    public List<Map<String, Object>> getPlaceDistribution() {
        return placeDistribution;
    }

    public void setPlaceDistribution(List<Map<String, Object>> placeDistribution) {
        this.placeDistribution = placeDistribution;
    }

    public List<Map<String, Object>> getRecentTrend() {
        return recentTrend;
    }

    public void setRecentTrend(List<Map<String, Object>> recentTrend) {
        this.recentTrend = recentTrend;
    }
}
