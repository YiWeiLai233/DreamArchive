package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.DreamStats;
import com.yiweilai.DreamArchive.service.StatsService;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    /**
     * 获取用户的完整梦境统计（实时查询）
     * GET /api/stats/{userId}
     */
    @GetMapping("/{userId}")
    public Result<DreamStats> getDreamStats(@PathVariable Integer userId) {
        return statsService.getDreamStats(userId);
    }

    /**
     * 获取用户的完整梦境统计（从统计表查询）
     * GET /api/stats/{userId}/cached
     */
    @GetMapping("/{userId}/cached")
    public Result<DreamStats> getDreamStatsFromTable(@PathVariable Integer userId) {
        return statsService.getDreamStatsFromTable(userId);
    }

    /**
     * 获取用户的梦境总数
     * GET /api/stats/{userId}/total
     */
    @GetMapping("/{userId}/total")
    public Result<Integer> getTotalDreams(@PathVariable Integer userId) {
        return statsService.getTotalDreams(userId);
    }

    /**
     * 获取情绪分布统计
     * GET /api/stats/{userId}/emotion
     */
    @GetMapping("/{userId}/emotion")
    public Result<List<Map<String, Object>>> getEmotionDistribution(@PathVariable Integer userId) {
        return statsService.getEmotionDistribution(userId);
    }

    /**
     * 获取地点分布统计
     * GET /api/stats/{userId}/place
     */
    @GetMapping("/{userId}/place")
    public Result<List<Map<String, Object>>> getPlaceDistribution(@PathVariable Integer userId) {
        return statsService.getPlaceDistribution(userId);
    }

    /**
     * 获取最近N天的趋势
     * GET /api/stats/{userId}/trend?days=7
     */
    @GetMapping("/{userId}/trend")
    public Result<List<Map<String, Object>>> getRecentTrend(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "7") Integer days) {
        return statsService.getRecentTrend(userId, days);
    }

    /**
     * 获取用户的连续记录天数
     * GET /api/stats/{userId}/streak
     */
    @GetMapping("/{userId}/streak")
    public Result<Integer> getStreak(@PathVariable Integer userId) {
        return statsService.getStreak(userId);
    }

    /**
     * 获取用户最长梦境的字数
     * GET /api/stats/{userId}/longest
     */
    @GetMapping("/{userId}/longest")
    public Result<Integer> getLongestDream(@PathVariable Integer userId) {
        return statsService.getLongestDream(userId);
    }
}
