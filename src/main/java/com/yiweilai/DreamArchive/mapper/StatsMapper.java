package com.yiweilai.DreamArchive.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {

    /**
     * 获取用户的总梦境数
     */
    Integer countByUserId(@Param("userId") Integer userId);

    /**
     * 获取用户的梦境总数（按情绪统计）
     */
    List<Map<String, Object>> countByEmotion(@Param("userId") Integer userId);

    /**
     * 获取用户的梦境总数（按地点统计）
     */
    List<Map<String, Object>> countByPlace(@Param("userId") Integer userId);

    /**
     * 获取最近N天的梦境数量趋势
     */
    List<Map<String, Object>> countRecentDays(@Param("userId") Integer userId, @Param("days") Integer days);

    /**
     * 获取用户所有记录梦境的日期（去重，按日期降序）
     */
    List<String> selectDreamDates(@Param("userId") Integer userId);

    /**
     * 获取用户最长梦境的内容字数
     */
    Integer selectLongestDreamContentLength(@Param("userId") Integer userId);
}
