package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.DreamStats;
import com.yiweilai.DreamArchive.DTO.DreamStatsEntity;
import com.yiweilai.DreamArchive.DTO.DreamPlaceStats;
import com.yiweilai.DreamArchive.mapper.StatsMapper;
import com.yiweilai.DreamArchive.mapper.DreamStatsMapper;
import com.yiweilai.DreamArchive.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsService.class);

    @Autowired
    private StatsMapper statsMapper;

    @Autowired
    private DreamStatsMapper dreamStatsMapper;

    /**
     * 获取用户的梦境统计数据（从 dream_stats 表读取）
     */
    public Result<DreamStats> getDreamStats(Integer userId) {
        try {
            // 从统计表获取最近7天的数据
            List<DreamStatsEntity> recentStats = dreamStatsMapper.selectRecentDays(userId, 7);

            // 总梦境数
            Integer totalDreams = statsMapper.countByUserId(userId);
            if (totalDreams == null) {
                totalDreams = 0;
            }

            // 情绪分布（从统计表聚合）
            List<Map<String, Object>> emotionDistribution = buildEmotionDistribution(recentStats);

            // 地点分布（从地点统计表获取）
            List<Map<String, Object>> placeDistribution = buildPlaceDistribution(userId);

            // 趋势数据
            List<Map<String, Object>> recentTrend = recentStats.stream()
                    .map(rs -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("date", rs.getStatDate().toString());
                        map.put("count", rs.getTotalDreams());
                        return map;
                    })
                    .collect(java.util.stream.Collectors.toList());

            DreamStats stats = new DreamStats(userId, totalDreams, emotionDistribution, placeDistribution, recentTrend);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return Result.error("获取统计数据失败，请稍后重试");
        }
    }

    /**
     * 获取用户的梦境统计数据（从统计表查询）— 与 getDreamStats 相同
     */
    public Result<DreamStats> getDreamStatsFromTable(Integer userId) {
        return getDreamStats(userId);
    }

    /**
     * 更新统计数据（当用户添加梦境时调用）
     */
    @Transactional
    public void updateStatsOnNewDream(Integer userId, String emotion, String place) {
        LocalDate today = LocalDate.now();

        // 更新每日统计
        dreamStatsMapper.incrementTotalDreams(userId, today);

        // 更新情绪统计
        if (emotion != null && !emotion.isEmpty()) {
            dreamStatsMapper.incrementEmotionCount(userId, today, emotion);
        }

        // 更新地点统计
        if (place != null && !place.isEmpty()) {
            dreamStatsMapper.incrementPlaceCount(userId, place);
        }
    }

    /**
     * 获取用户的梦境总数（从统计表）
     */
    @Transactional
    public void rebuildStatsAfterDreamDeleted(Integer userId, LocalDate statDate) {
        if (userId == null || statDate == null) {
            return;
        }

        dreamStatsMapper.deleteDailyStats(userId, statDate);
        dreamStatsMapper.rebuildDailyStats(userId, statDate);
        dreamStatsMapper.deletePlaceStatsByUser(userId);
        dreamStatsMapper.rebuildPlaceStatsByUser(userId);
    }

    public Result<Integer> getTotalDreams(Integer userId) {
        try {
            Integer total = statsMapper.countByUserId(userId);
            if (total == null) {
                total = 0;
            }
            return Result.success(total);
        } catch (Exception e) {
            log.error("获取梦境总数失败", e);
            return Result.error("获取数据失败，请稍后重试");
        }
    }

    /**
     * 获取情绪分布统计（从统计表聚合）
     */
    public Result<List<Map<String, Object>>> getEmotionDistribution(Integer userId) {
        try {
            List<DreamStatsEntity> allStats = dreamStatsMapper.selectRecentDays(userId, 365);
            List<Map<String, Object>> distribution = buildEmotionDistribution(allStats);
            return Result.success(distribution);
        } catch (Exception e) {
            log.error("获取情绪分布失败", e);
            return Result.error("获取数据失败，请稍后重试");
        }
    }

    /**
     * 获取地点分布统计（从地点统计表读取）
     */
    public Result<List<Map<String, Object>>> getPlaceDistribution(Integer userId) {
        try {
            List<Map<String, Object>> distribution = buildPlaceDistribution(userId);
            return Result.success(distribution);
        } catch (Exception e) {
            log.error("获取地点分布失败", e);
            return Result.error("获取数据失败，请稍后重试");
        }
    }

    /**
     * 获取最近N天的趋势（从统计表）
     */
    public Result<List<Map<String, Object>>> getRecentTrend(Integer userId, Integer days) {
        try {
            List<DreamStatsEntity> recentStats = dreamStatsMapper.selectRecentDays(userId, days);
            List<Map<String, Object>> trend = recentStats.stream()
                    .map(rs -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("date", rs.getStatDate().toString());
                        map.put("count", rs.getTotalDreams());
                        return map;
                    })
                    .collect(java.util.stream.Collectors.toList());
            return Result.success(trend);
        } catch (Exception e) {
            log.error("获取趋势数据失败", e);
            return Result.error("获取数据失败，请稍后重试");
        }
    }

    /**
     * 从统计表聚合情绪分布
     */
    private List<Map<String, Object>> buildEmotionDistribution(List<DreamStatsEntity> statsList) {
        int happy = 0, sad = 0, scared = 0, angry = 0, peaceful = 0, other = 0;
        for (DreamStatsEntity s : statsList) {
            happy += s.getHappyCount() != null ? s.getHappyCount() : 0;
            sad += s.getSadCount() != null ? s.getSadCount() : 0;
            scared += s.getScaredCount() != null ? s.getScaredCount() : 0;
            angry += s.getAngryCount() != null ? s.getAngryCount() : 0;
            peaceful += s.getPeacefulCount() != null ? s.getPeacefulCount() : 0;
            other += s.getOtherEmotionCount() != null ? s.getOtherEmotionCount() : 0;
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        if (happy > 0) result.add(Map.of("label", "开心", "value", happy));
        if (sad > 0) result.add(Map.of("label", "难过", "value", sad));
        if (scared > 0) result.add(Map.of("label", "恐惧", "value", scared));
        if (angry > 0) result.add(Map.of("label", "愤怒", "value", angry));
        if (peaceful > 0) result.add(Map.of("label", "平静", "value", peaceful));
        if (other > 0) result.add(Map.of("label", "其他", "value", other));
        return result;
    }

    /**
     * 获取用户连续记录天数
     * 从今天（或昨天）开始往前数，连续有梦境记录的天数
     */
    public Result<Integer> getStreak(Integer userId) {
        try {
            List<String> dates = statsMapper.selectDreamDates(userId);
            if (dates == null || dates.isEmpty()) {
                return Result.success(0);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate today = LocalDate.now();

            // 尝试从今天开始计算连续天数
            int streak = countConsecutive(dates, today, fmt);

            // 如果今天没有记录，尝试从昨天开始
            if (streak == 0) {
                streak = countConsecutive(dates, today.minusDays(1), fmt);
            }

            return Result.success(streak);
        } catch (Exception e) {
            log.error("获取连续记录失败", e);
            return Result.error("获取数据失败，请稍后重试");
        }
    }

    private int countConsecutive(List<String> dates, LocalDate startDate, DateTimeFormatter fmt) {
        int streak = 0;
        for (String dateStr : dates) {
            LocalDate date = LocalDate.parse(dateStr, fmt);
            if (date.equals(startDate.minusDays(streak))) {
                streak++;
            } else if (date.isBefore(startDate.minusDays(streak))) {
                break;
            }
        }
        return streak;
    }

    /**
     * 获取用户最长梦境的字数
     */
    public Result<Integer> getLongestDream(Integer userId) {
        try {
            Integer length = statsMapper.selectLongestDreamContentLength(userId);
            return Result.success(length != null ? length : 0);
        } catch (Exception e) {
            log.error("获取最长梦境失败", e);
            return Result.error("获取数据失败，请稍后重试");
        }
    }

    /**
     * 从地点统计表获取分布
     */
    private List<Map<String, Object>> buildPlaceDistribution(Integer userId) {
        List<DreamPlaceStats> placeStats = dreamStatsMapper.selectPlaceStatsByUser(userId);
        return placeStats.stream()
                .map(ps -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("label", ps.getPlace());
                    map.put("value", ps.getDreamCount());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
