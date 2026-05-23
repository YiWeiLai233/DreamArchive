package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.DreamStats;
import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.service.StatsService;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        if (!canAccessUser(userId)) {
            return Result.error(403, "\u6743\u9650\u4e0d\u8db3");
        }
        return statsService.getDreamStats(userId);
    }

    /**
     * 获取用户的完整梦境统计（从统计表查询）
     * GET /api/stats/{userId}/cached
     */
    @GetMapping("/{userId}/cached")
    public Result<DreamStats> getDreamStatsFromTable(@PathVariable Integer userId) {
        if (!canAccessUser(userId)) {
            return Result.error(403, "\u6743\u9650\u4e0d\u8db3");
        }
        return statsService.getDreamStatsFromTable(userId);
    }

    /**
     * 获取用户的梦境总数
     * GET /api/stats/{userId}/total
     */
    @GetMapping("/{userId}/total")
    public Result<Integer> getTotalDreams(@PathVariable Integer userId) {
        if (!canAccessUser(userId)) {
            return Result.error(403, "\u6743\u9650\u4e0d\u8db3");
        }
        return statsService.getTotalDreams(userId);
    }

    /**
     * 获取情绪分布统计
     * GET /api/stats/{userId}/emotion
     */
    @GetMapping("/{userId}/emotion")
    public Result<List<Map<String, Object>>> getEmotionDistribution(@PathVariable Integer userId) {
        if (!canAccessUser(userId)) {
            return Result.error(403, "\u6743\u9650\u4e0d\u8db3");
        }
        return statsService.getEmotionDistribution(userId);
    }

    /**
     * 获取地点分布统计
     * GET /api/stats/{userId}/place
     */
    @GetMapping("/{userId}/place")
    public Result<List<Map<String, Object>>> getPlaceDistribution(@PathVariable Integer userId) {
        if (!canAccessUser(userId)) {
            return Result.error(403, "\u6743\u9650\u4e0d\u8db3");
        }
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
        if (!canAccessUser(userId)) {
            return Result.error(403, "\u6743\u9650\u4e0d\u8db3");
        }
        return statsService.getRecentTrend(userId, days);
    }

    /**
     * 获取用户的连续记录天数
     * GET /api/stats/{userId}/streak
     */
    @GetMapping("/{userId}/streak")
    public Result<Integer> getStreak(@PathVariable Integer userId) {
        if (!canAccessUser(userId)) {
            return Result.error(403, "\u6743\u9650\u4e0d\u8db3");
        }
        return statsService.getStreak(userId);
    }

    /**
     * 获取用户最长梦境的字数
     * GET /api/stats/{userId}/longest
     */
    @GetMapping("/{userId}/longest")
    public Result<Integer> getLongestDream(@PathVariable Integer userId) {
        if (!canAccessUser(userId)) {
            return Result.error(403, "\u6743\u9650\u4e0d\u8db3");
        }
        return statsService.getLongestDream(userId);
    }

    private boolean canAccessUser(Integer userId) {
        User currentUser = currentUser();
        return currentUser != null
                && userId != null
                && (isAdmin(currentUser) || currentUser.getId() == userId);
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }
}
