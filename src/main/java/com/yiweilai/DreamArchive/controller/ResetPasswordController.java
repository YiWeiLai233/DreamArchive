package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.service.ClientIpResolver;
import com.yiweilai.DreamArchive.service.RateLimitExceededException;
import com.yiweilai.DreamArchive.service.ResetPasswordService;
import com.yiweilai.DreamArchive.service.SecurityRateLimitService;
import com.yiweilai.DreamArchive.service.VerificationCodeService;
import com.yiweilai.DreamArchive.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ResetPasswordController {
    private static final String RESET_CODE_PUBLIC_MESSAGE =
            "如果账号存在，验证码已发送至绑定邮箱";

    @Autowired
    private ResetPasswordService resetPasswordService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private LoginMapper loginMapper;

    @Autowired
    private SecurityRateLimitService rateLimitService;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @PostMapping("/reset-password/send-code")
    public Result<String> sendCode(@RequestBody Map<String, String> request, HttpServletRequest servletRequest) {
        String identifier = request.get("identifier");
        if (identifier != null) {
            identifier = identifier.trim();
        }

        if (identifier == null || identifier.isEmpty()) {
            return Result.error("请输入用户名或邮箱");
        }
        if (isVerificationSendLimited("reset-password", servletRequest)) {
            return Result.error(429, SecurityRateLimitService.RATE_LIMIT_MESSAGE);
        }

        // 查找用户邮箱
        String email = null;
        if (identifier.contains("@")) {
            User user = loginMapper.selectByEmail(identifier);
            if (user != null) {
                email = user.getEmail();
            }
        } else {
            User user = loginMapper.selectByUsername(identifier);
            if (user != null) {
                email = user.getEmail();
            }
        }

        if (email == null || email.isBlank()) {
            return Result.success(RESET_CODE_PUBLIC_MESSAGE);
        }
        try {
            verificationCodeService.sendCode("reset-password", identifier, email);
            return Result.success(RESET_CODE_PUBLIC_MESSAGE);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody Map<String, String> request, HttpServletRequest servletRequest) {
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
        try {
            if (!verificationCodeService.verifyCode("reset-password", identifier, code, resolveClientIp(servletRequest))) {
                return Result.error("验证码错误或已过期");
            }
        } catch (RateLimitExceededException e) {
            return Result.error(429, e.getMessage());
        }

        return resetPasswordService.resetPassword(identifier, newPassword);
    }

    private boolean isVerificationSendLimited(String scene, HttpServletRequest request) {
        return rateLimitService != null
                && rateLimitService.consumeVerificationSend(scene, resolveClientIp(request));
    }

    private String resolveClientIp(HttpServletRequest request) {
        return clientIpResolver == null ? "unknown" : clientIpResolver.resolve(request);
    }
}
