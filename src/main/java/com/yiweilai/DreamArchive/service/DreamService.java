package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.DreamContent;
import com.yiweilai.DreamArchive.DTO.PagedDreamContentResponse;
import com.yiweilai.DreamArchive.mapper.DreamContentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DreamService {

    private static final int DEFAULT_DREAM_PAGE_SIZE = 9;
    private static final int MAX_DREAM_PAGE_SIZE = 30;

    @Autowired
    private DreamContentMapper dreamContentMapper;

    @Autowired
    private StatsService statsService;

    @Autowired
    private MinioService minioService;

    /**
     * 保存梦境并更新统计数据
     * 1. 保存梦境内容到 dream_content 表
     * 2. 更新情绪统计到 dream_stats 表
     * 3. 更新地点统计到 dream_place_stats 表
     */
    @Transactional
    public DreamContent saveDream(Integer userId, String title, String content,
                                   String emotion, String place, String time, String imageUrl) {
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
        dreamContent.setAnalysisStatus("NONE");
        dreamContent.setImageUrl(imageUrl);
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

    public void updateAnalysisStatus(String dreamId, String status, String error) {
        dreamContentMapper.updateAnalysisStatus(dreamId, status, error);
    }

    public void updateTitle(String dreamId, String title) {
        dreamContentMapper.updateTitle(dreamId, title);
    }

    @Transactional
    public DreamContent updateEditableDream(String id, Integer userId, String title, String content,
                                            String emotion, String place, String time) {
        DreamContent existingDream = dreamContentMapper.selectById(id);
        if (existingDream == null
                || userId == null
                || !userId.equals(existingDream.getUserId())
                || !isEditableDream(existingDream)) {
            return null;
        }

        String nextContent = content == null ? "" : content.trim();
        String nextEmotion = emotion == null ? "" : emotion.trim();
        if (nextContent.isBlank() || nextEmotion.isBlank()) {
            return null;
        }

        String nextTitle = title == null ? "" : title.trim();
        if (nextTitle.isBlank()) {
            nextTitle = fallbackTitle(nextContent);
        }
        String nextPlace = place == null || place.isBlank() ? "未知" : place.trim();
        String nextTime = time == null ? "" : time.trim();

        int updated = dreamContentMapper.updateEditableDream(id, nextTitle, nextContent, nextEmotion, nextPlace, nextTime);
        if (updated <= 0) {
            return null;
        }

        LocalDate statDate = existingDream.getCreatedAt() == null
                ? LocalDate.now()
                : existingDream.getCreatedAt().toLocalDate();
        statsService.rebuildStatsAfterDreamDeleted(existingDream.getUserId(), statDate);

        DreamContent updatedDream = dreamContentMapper.selectById(id);
        if (updatedDream == null) {
            updatedDream = existingDream;
        }
        updatedDream.setTitle(nextTitle);
        updatedDream.setContent(nextContent);
        updatedDream.setEmotion(nextEmotion);
        updatedDream.setPlace(nextPlace);
        updatedDream.setTime(nextTime);
        updatedDream.setInterpretation("");
        updatedDream.setAnalysisStatus("NONE");
        updatedDream.setAnalysisError(null);
        return updatedDream;
    }

    public boolean isEditableDream(DreamContent dream) {
        if (dream == null) {
            return false;
        }
        String status = dream.getAnalysisStatus();
        if (status == null || status.isBlank()) {
            String interpretation = dream.getInterpretation();
            return interpretation == null || interpretation.isBlank();
        }
        return !"PENDING".equalsIgnoreCase(status) && !"SUCCESS".equalsIgnoreCase(status);
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

    public PagedDreamContentResponse getDreamsPageByUserId(Integer userId, int page, int pageSize, String keyword, String filter) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = pageSize <= 0
                ? DEFAULT_DREAM_PAGE_SIZE
                : Math.min(pageSize, MAX_DREAM_PAGE_SIZE);
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        String normalizedFilter = filter == null ? "" : filter.trim();
        boolean draftsOnly = "drafts".equalsIgnoreCase(normalizedFilter);
        String emotion = normalizedFilter.isBlank()
                || "all".equalsIgnoreCase(normalizedFilter)
                || draftsOnly
                ? null
                : normalizedFilter;

        long total = dreamContentMapper.countByUserIdWithFilters(userId, normalizedKeyword, emotion, draftsOnly);
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) normalizedPageSize));
        int currentPage = Math.min(normalizedPage, totalPages);
        if (total <= 0) {
            return new PagedDreamContentResponse(List.of(), 0L, currentPage, normalizedPageSize, totalPages);
        }

        int offset = (currentPage - 1) * normalizedPageSize;
        List<DreamContent> items = dreamContentMapper.selectPageByUserId(
                userId,
                normalizedKeyword,
                emotion,
                draftsOnly,
                normalizedPageSize,
                offset
        );
        return new PagedDreamContentResponse(items, total, currentPage, normalizedPageSize, totalPages);
    }

    @Transactional
    public boolean deleteDream(String id) {
        DreamContent existingDream = dreamContentMapper.selectById(id);
        if (existingDream == null) {
            return false;
        }

        boolean deleted = dreamContentMapper.deleteById(id) > 0;
        if (deleted) {
            LocalDate statDate = existingDream.getCreatedAt() == null
                    ? LocalDate.now()
                    : existingDream.getCreatedAt().toLocalDate();
            statsService.rebuildStatsAfterDreamDeleted(existingDream.getUserId(), statDate);
            // 删除 MinIO 中的图片
            if (existingDream.getImageUrl() != null && !existingDream.getImageUrl().isEmpty()) {
                String objectName = minioService.extractObjectName(existingDream.getImageUrl());
                minioService.deleteObject(objectName);
            }
        }
        return deleted;
    }

    private String fallbackTitle(String content) {
        String compact = content == null ? "" : content.replaceAll("\\s+", "").trim();
        if (compact.isBlank()) {
            return "未命名梦境";
        }
        return compact.length() > 12 ? compact.substring(0, 12) : compact;
    }

    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
