package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.service.ResetPasswordService;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ResetPasswordController {

    @Autowired
    private ResetPasswordService resetPasswordService;

    /**
     * 发送验证码（暂时跳过验证码验证，直接返回成功）
     */
    @PostMapping("/reset-password/send-code")
    public Result<String> sendCode(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");

        if (identifier == null || identifier.isEmpty()) {
            return Result.error("请输入用户名或邮箱");
        }

        // 检查用户是否存在
        boolean exists = resetPasswordService.checkUserExists(identifier);
        if (!exists) {
            return Result.error("用户不存在");
        }

        // TODO: 实际发送验证码（邮件/短信）
        // 暂时直接返回成功
        return Result.success("验证码已发送");
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String code = request.get("code");
        String newPassword = request.get("newPassword");

        if (identifier == null || identifier.isEmpty()) {
            return Result.error("请输入用户名或邮箱");
        }
        if (code == null || code.isEmpty()) {
            return Result.error("请输入验证码");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.error("请输入新密码");
        }
        if (newPassword.length() < 6) {
            return Result.error("密码长度不能少于6位");
        }

        // TODO: 验证验证码（暂时跳过验证）
        // 后续可添加 Redis 存储验证码并验证

        return resetPasswordService.resetPassword(identifier, newPassword);
    }
}
