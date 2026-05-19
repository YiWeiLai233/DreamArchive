package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.DreamContent;
import com.yiweilai.DreamArchive.service.DreamAiTaskService;
import com.yiweilai.DreamArchive.service.AiService;
import com.yiweilai.DreamArchive.service.DreamService;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@CrossOrigin(origins = "*")
public class analyzeDream {

    @Autowired
    private DreamService dreamService;

    @Autowired
    private AiService aiService;

    @Autowired
    private DreamAiTaskService dreamAiTaskService;

    @PostMapping("/dream/analyze")
    public Result<Map<String, String>> analyzeDreamOnly(@RequestBody Map<String, Object> request) {
        try {
            String content = toStringValue(request.get("content"));
            String interpretation = aiService.analyzeDream(content);
            return Result.success(Map.of("interpretation", interpretation));
        } catch (Exception e) {
            return Result.error("AI 解析梦境失败: " + e.getMessage());
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
            return Result.success(dream);
        } catch (Exception e) {
            return Result.error("查询梦境失败: " + e.getMessage());
        }
    }

    @GetMapping("/dreams/user/{userId}")
    public Result<List<DreamContent>> getUserDreams(@PathVariable Integer userId) {
        try {
            List<DreamContent> dreams = dreamService.getDreamsWithDetailsByUserId(userId);
            return Result.success(dreams);
        } catch (Exception e) {
            return Result.error("查询梦境列表失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/dream/{id}")
    public Result<Void> deleteDream(@PathVariable String id) {
        try {
            boolean deleted = dreamService.deleteDream(id);
            if (!deleted) {
                return Result.error("梦境不存在或已被删除");
            }
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error("删除梦境失败: " + e.getMessage());
        }
    }

    private Result<DreamContent> saveDreamInternal(Map<String, Object> request, boolean analyze) {
        try {
            Integer userId = toInteger(request.get("userId"));
            String content = toStringValue(request.get("content"));
            String title = toStringValue(request.getOrDefault("title", "")).trim();
            String emotion = toStringValue(request.get("emotion"));
            String place = toStringValue(request.getOrDefault("place", "未知"));
            String time = toStringValue(request.getOrDefault("time", ""));
            boolean shouldGenerateTitle = title.isBlank();
            if (shouldGenerateTitle) {
                title = fallbackTitle(content);
            }

            DreamContent result = dreamService.saveDream(userId, title, content, emotion, place, time);
            if (analyze) {
                String pending = "AI 正在后台解析中，稍后可在梦境列表查看结果。";
                dreamService.updateInterpretation(result.getId(), pending);
                result.setInterpretation(pending);
            }
            if (shouldGenerateTitle || analyze) {
                dreamAiTaskService.completeDreamAiFields(result.getId(), content, shouldGenerateTitle, analyze);
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("保存梦境失败: " + e.getMessage());
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
