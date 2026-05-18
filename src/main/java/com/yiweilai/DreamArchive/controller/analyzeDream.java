package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.DreamContent;
import com.yiweilai.DreamArchive.service.DreamService;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class analyzeDream {

    @Autowired
    private DreamService dreamService;

    /**
     * 保存梦境
     * 请求体: { userId, title, content, emotion, place, time }
     */
    @PostMapping("/analysisDream")
    public Result<DreamContent> saveDream(@RequestBody Map<String, Object> request) {
        try {
            Integer userId = (Integer) request.get("userId");
            String title = (String) request.getOrDefault("title", "");
            String content = (String) request.get("content");
            String emotion = (String) request.get("emotion");
            String place = (String) request.getOrDefault("place", "未知");
            String time = (String) request.getOrDefault("time", "");

            DreamContent result = dreamService.saveDream(userId, title, content, emotion, place, time);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("保存梦境失败: " + e.getMessage());
        }
    }

    /**
     * 根据 ID 查询梦境
     */
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

    /**
     * 查询用户的所有梦境
     */
    @GetMapping("/dreams/user/{userId}")
    public Result<List<DreamContent>> getUserDreams(@PathVariable Integer userId) {
        try {
            List<DreamContent> dreams = dreamService.getDreamsWithDetailsByUserId(userId);
            return Result.success(dreams);
        } catch (Exception e) {
            return Result.error("查询梦境列表失败: " + e.getMessage());
        }
    }
}
