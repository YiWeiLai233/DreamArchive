package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.DreamContent;
import com.yiweilai.DreamArchive.mapper.DreamContentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DreamService {

    @Autowired
    private DreamContentMapper dreamContentMapper;

    @Autowired
    private StatsService statsService;

    /**
     * 保存梦境并更新统计数据
     * 1. 保存梦境内容到 dream_content 表
     * 2. 更新情绪统计到 dream_stats 表
     * 3. 更新地点统计到 dream_place_stats 表
     */
    @Transactional
    public DreamContent saveDream(Integer userId, String title, String content,
                                   String emotion, String place, String time) {
        String dreamId = UUID.randomUUID().toString();

        // 1. 保存梦境内容
        DreamContent dreamContent = new DreamContent();
        dreamContent.setId(dreamId);
        dreamContent.setUserId(userId);
        dreamContent.setTitle(title);
        dreamContent.setContent(content);
        dreamContent.setEmotion(emotion);
        dreamContent.setPlace(place);
        dreamContent.setTime(time);
        dreamContent.setInterpretation("");
        dreamContentMapper.insertDreamContent(dreamContent);

        // 2. 更新统计表（情绪 + 地点）
        statsService.updateStatsOnNewDream(userId, emotion, place);

        return dreamContent;
    }

    /**
     * 更新梦境的 AI 解析结果
     */
    public void updateInterpretation(String dreamId, String interpretation) {
        dreamContentMapper.updateInterpretation(dreamId, interpretation);
    }

    /**
     * 根据 ID 查询梦境
     */
    public DreamContent getDreamById(String id) {
        return dreamContentMapper.selectById(id);
    }

    /**
     * 查询用户的所有梦境
     */
    public List<DreamContent> getDreamsByUserId(Integer userId) {
        return dreamContentMapper.selectByUserId(userId);
    }

    /**
     * 查询用户的所有梦境（从 dream_content 表）
     */
    public List<DreamContent> getDreamsWithDetailsByUserId(Integer userId) {
        return dreamContentMapper.selectByUserId(userId);
    }

    @Transactional
    public boolean deleteDream(String id) {
        DreamContent existingDream = dreamContentMapper.selectById(id);
        if (existingDream == null) {
            return false;
        }
        return dreamContentMapper.deleteById(id) > 0;
    }
}
