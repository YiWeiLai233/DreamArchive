package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.DreamContent;
import com.yiweilai.DreamArchive.service.AiService;
import com.yiweilai.DreamArchive.service.DreamService;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
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

    @PostMapping("/analysisDream")
    public Result<DreamContent> saveDream(@RequestBody Map<String, Object> request) {
        try {
            Integer userId = toInteger(request.get("userId"));
            String title = toStringValue(request.getOrDefault("title", ""));
            String content = toStringValue(request.get("content"));
            String emotion = toStringValue(request.get("emotion"));
            String place = toStringValue(request.getOrDefault("place", "未知"));
            String time = toStringValue(request.getOrDefault("time", ""));
            boolean analyze = Boolean.TRUE.equals(request.get("analyze"));

            DreamContent result = dreamService.saveDream(userId, title, content, emotion, place, time);
            if (analyze) {
                try {
                    String interpretation = aiService.analyzeDream(content);
                    dreamService.updateInterpretation(result.getId(), interpretation);
                    result.setInterpretation(interpretation);
                } catch (Exception aiException) {
                    result.setInterpretation("AI 解析暂时失败，梦境已保存。你可以稍后重试或查看梦境记录。");
                }
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("保存梦境失败: " + e.getMessage());
        }
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
}
