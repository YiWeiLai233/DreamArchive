package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.DreamContent;
import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.service.DreamAiTaskService;
import com.yiweilai.DreamArchive.service.AiService;
import com.yiweilai.DreamArchive.service.DreamService;
import com.yiweilai.DreamArchive.service.MinioService;
import com.yiweilai.DreamArchive.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DreamController {

    private static final Logger log = LoggerFactory.getLogger(DreamController.class);

    @Autowired
    private DreamService dreamService;

    @Autowired
    private AiService aiService;

    @Autowired
    private DreamAiTaskService dreamAiTaskService;

    @Autowired
    private MinioService minioService;

    @PostMapping("/dream/analyze")
    public Result<Map<String, String>> analyzeDreamOnly(@RequestBody Map<String, Object> request) {
        try {
            String content = toStringValue(request.get("content"));
            String interpretation = aiService.analyzeDream(content);
            return Result.success(Map.of("interpretation", interpretation));
        } catch (Exception e) {
            log.error("AI 解析梦境失败", e);
            return Result.error("AI 解析失败，请稍后重试");
        }
    }

    @PostMapping("/dreams/save-and-analyze")
    public Result<DreamContent> saveAndAnalyzeDream(@RequestBody Map<String, Object> request) {
        return saveDreamInternal(request, true);
    }

    @PostMapping("/analysisDream")
    public Result<DreamContent> saveDream(@RequestBody Map<String, Object> request) {
        return saveDreamInternal(request, toBoolean(request.get("analyze")));
    }

    @GetMapping("/dream/{id}")
    public Result<DreamContent> getDream(@PathVariable String id) {
        try {
            DreamContent dream = dreamService.getDreamById(id);
            if (dream == null) {
                return Result.error("梦境不存在");
            }
            if (!canAccessDream(dream)) {
                return Result.error(403, "\u6743\u9650\u4e0d\u8db3");
            }
            enrichImageUrl(dream);
            return Result.success(dream);
        } catch (Exception e) {
            log.error("查询梦境失败", e);
            return Result.error("查询失败，请稍后重试");
        }
    }

    @GetMapping("/dreams/user/{userId}")
    public Result<List<DreamContent>> getUserDreams(@PathVariable Integer userId) {
        try {
            if (!canAccessUser(userId)) {
                return Result.error(403, "\u6743\u9650\u4e0d\u8db3");
            }
            List<DreamContent> dreams = dreamService.getDreamsWithDetailsByUserId(userId);
            enrichImageUrls(dreams);
            return Result.success(dreams);
        } catch (Exception e) {
            log.error("查询梦境列表失败", e);
            return Result.error("查询失败，请稍后重试");
        }
    }

    @PostMapping("/dream/{id}/delete")
    public Result<Void> deleteDream(@PathVariable String id) {
        try {
            DreamContent dream = dreamService.getDreamById(id);
            if (dream == null) {
                return Result.error("梦境不存在或已被删除");
            }
            if (!canAccessDream(dream)) {
                return Result.error(403, "\u6743\u9650\u4e0d\u8db3");
            }
            boolean deleted = dreamService.deleteDream(id);
            if (!deleted) {
                return Result.error("梦境不存在或已被删除");
            }
            return Result.success("删除成功", null);
        } catch (Exception e) {
            log.error("删除梦境失败", e);
            return Result.error("删除失败，请稍后重试");
        }
    }

    @PostMapping("/dream/{id}/update")
    public Result<DreamContent> updateDream(@PathVariable String id, @RequestBody Map<String, Object> request) {
        try {
            DreamContent dream = dreamService.getDreamById(id);
            if (dream == null) {
                return Result.error("梦境不存在或已被删除");
            }
            if (!canAccessDream(dream)) {
                return Result.error(403, "权限不足");
            }
            if (!dreamService.isEditableDream(dream)) {
                return Result.error("已解析或解析中的梦境不能编辑");
            }

            String content = toStringValue(request.get("content")).trim();
            String emotion = toStringValue(request.get("emotion")).trim();
            if (content.isBlank() || emotion.isBlank()) {
                return Result.error("梦境内容和情绪不能为空");
            }

            DreamContent updated = dreamService.updateEditableDream(
                    id,
                    dream.getUserId(),
                    toStringValue(request.getOrDefault("title", "")),
                    content,
                    emotion,
                    toStringValue(request.getOrDefault("place", "未知")),
                    toStringValue(request.getOrDefault("time", ""))
            );
            if (updated == null) {
                return Result.error("更新失败，请确认梦境仍可编辑");
            }
            enrichImageUrl(updated);
            return Result.success(updated);
        } catch (Exception e) {
            log.error("更新梦境失败", e);
            return Result.error("更新失败，请稍后重试");
        }
    }

    @PostMapping("/dream/{id}/analyze")
    public Result<Void> analyzeDream(@PathVariable String id) {
        try {
            DreamContent dream = dreamService.getDreamById(id);
            if (dream == null) {
                return Result.error("梦境不存在或已被删除");
            }
            if (!canAccessDream(dream)) {
                return Result.error(403, "权限不足");
            }
            dreamService.updateAnalysisStatus(id, "PENDING", null);
            dreamAiTaskService.completeDreamAiFields(id, dream.getContent(), dream.getImageUrl(), dream.getEmotion(), dream.getPlace(), dream.getTime(), false, true);
            return Result.success("已提交解析", null);
        } catch (Exception e) {
            log.error("提交解析失败", e);
            return Result.error("提交失败，请稍后重试");
        }
    }

    private Result<DreamContent> saveDreamInternal(Map<String, Object> request, boolean analyze) {
        try {
            User currentUser = currentUser();
            if (currentUser == null) {
                return Result.error(401, "\u8bf7\u5148\u767b\u5f55");
            }
            Integer userId = currentUser.getId();
            String content = toStringValue(request.get("content"));
            String title = toStringValue(request.getOrDefault("title", "")).trim();
            String emotion = toStringValue(request.get("emotion"));
            String place = toStringValue(request.getOrDefault("place", "未知"));
            String time = toStringValue(request.getOrDefault("time", ""));
            String interpretation = toStringValue(request.getOrDefault("interpretation", ""));
            String imageUrl = toStringValue(request.getOrDefault("imageUrl", ""));
            if (imageUrl.isBlank()) imageUrl = null;
            boolean shouldGenerateTitle = title.isBlank();
            if (shouldGenerateTitle) {
                title = fallbackTitle(content);
            }

            DreamContent result = dreamService.saveDream(userId, title, content, emotion, place, time, imageUrl);
            if (!interpretation.isBlank()) {
                dreamService.updateInterpretation(result.getId(), interpretation);
                result.setInterpretation(interpretation);
                result.setAnalysisStatus("SUCCESS");
            } else if (analyze) {
                dreamService.updateAnalysisStatus(result.getId(), "PENDING", null);
                result.setAnalysisStatus("PENDING");
            }
            if (shouldGenerateTitle || analyze) {
                dreamAiTaskService.completeDreamAiFields(result.getId(), content, imageUrl, emotion, place, time, shouldGenerateTitle, analyze);
            }
            enrichImageUrl(result);
            return Result.success(result);
        } catch (Exception e) {
            log.error("保存梦境失败", e);
            return Result.error("保存失败，请稍后重试");
        }
    }

    private void enrichImageUrl(DreamContent dream) {
        if (dream != null && dream.getImageUrl() != null && !dream.getImageUrl().isEmpty()) {
            String objectName = minioService.extractObjectName(dream.getImageUrl());
            String url = minioService.getPresignedUrl(objectName);
            if (url != null) {
                dream.setImageUrl(url);
            }
        }
    }

    private void enrichImageUrls(List<DreamContent> dreams) {
        if (dreams != null) {
            for (DreamContent d : dreams) {
                enrichImageUrl(d);
            }
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private boolean canAccessDream(DreamContent dream) {
        return dream != null && canAccessUser(dream.getUserId());
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

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private String fallbackTitle(String content) {
        String compact = content == null ? "" : content.replaceAll("\\s+", "").trim();
        if (compact.isBlank()) {
            return "未命名梦境";
        }
        return compact.length() > 12 ? compact.substring(0, 12) : compact;
    }
}
