package com.yiweilai.DreamArchive.mapper;

import com.yiweilai.DreamArchive.DTO.DreamPlaceStats;
import com.yiweilai.DreamArchive.DTO.DreamStatsEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DreamStatsMapper {

    /**
     * 获取用户某日的统计数据
     */
    DreamStatsEntity selectByUserAndDate(@Param("userId") Integer userId, @Param("statDate") LocalDate statDate);

    /**
     * 插入或更新每日统计
     */
    int insertOrUpdateDailyStats(DreamStatsEntity stats);

    /**
     * 增加总梦境数
     */
    int incrementTotalDreams(@Param("userId") Integer userId, @Param("statDate") LocalDate statDate);

    /**
     * 增加特定情绪的梦境数
     */
    int incrementEmotionCount(@Param("userId") Integer userId, @Param("statDate") LocalDate statDate, @Param("emotion") String emotion);

    /**
     * 获取用户的地点统计
     */
    DreamPlaceStats selectPlaceStats(@Param("userId") Integer userId, @Param("place") String place);

    /**
     * 插入或更新地点统计
     */
    int insertOrUpdatePlaceStats(DreamPlaceStats placeStats);

    /**
     * 增加地点的梦境数
     */
    int incrementPlaceCount(@Param("userId") Integer userId, @Param("place") String place);

    /**
     * 获取用户最近N天的统计
     */
    List<DreamStatsEntity> selectRecentDays(@Param("userId") Integer userId, @Param("days") Integer days);

    /**
     * 获取用户的地点统计列表
     */
    List<DreamPlaceStats> selectPlaceStatsByUser(@Param("userId") Integer userId);
}
