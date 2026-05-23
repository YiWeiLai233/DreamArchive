package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.service.ResetPasswordService;
import com.yiweilai.DreamArchive.service.VerificationCodeService;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ResetPasswordController {

    @Autowired
    private ResetPasswordService resetPasswordService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private LoginMapper loginMapper;

    @PostMapping("/reset-password/send-code")
    public Result<String> sendCode(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        if (identifier != null) {
            identifier = identifier.trim();
        }

        if (identifier == null || identifier.isEmpty()) {
            return Result.error("请输入用户名或邮箱");
        }

        // 查找用户邮箱
        String email;
        if (identifier.contains("@")) {
            email = identifier;
        } else {
            User user = loginMapper.selectByUsername(identifier);
            if (user == null) {
                return Result.error("用户不存在");
            }
            email = user.getEmail();
        }

        try {
            verificationCodeService.sendCode("reset-password", identifier, email);
            return Result.success("验证码已发送至您的邮箱");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String code = request.get("code");
        String newPassword = request.get("newPassword");
        if (identifier != null) {
            identifier = identifier.trim();
        }
        if (code != null) {
            code = code.trim();
        }

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

        // 验证验证码
        if (!verificationCodeService.verifyCode("reset-password", identifier, code)) {
            return Result.error("验证码错误或已过期");
        }

        return resetPasswordService.resetPassword(identifier, newPassword);
    }
}
