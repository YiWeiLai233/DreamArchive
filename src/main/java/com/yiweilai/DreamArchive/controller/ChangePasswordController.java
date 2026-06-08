package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.mapper.ResetPasswordMapper;
import com.yiweilai.DreamArchive.service.ClientIpResolver;
import com.yiweilai.DreamArchive.service.RateLimitExceededException;
import com.yiweilai.DreamArchive.service.SecurityRateLimitService;
import com.yiweilai.DreamArchive.service.VerificationCodeService;
import com.yiweilai.DreamArchive.util.Result;
import com.yiweilai.DreamArchive.util.SensitiveDataEncryptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChangePasswordController {

    @Autowired
    private LoginMapper loginMapper;

    @Autowired
    private ResetPasswordMapper resetPasswordMapper;

    @Autowired
    private SensitiveDataEncryptor sensitiveDataEncryptor;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private SecurityRateLimitService rateLimitService;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @PostMapping("/change-password/send-code")
    public Result<String> sendCode(@RequestBody Map<String, String> request, HttpServletRequest servletRequest) {
        User currentUser = currentUser();
        if (currentUser == null) {
            return Result.error(401, "请先登录");
        }
        String email = currentUser.getEmail();
        if (email == null || email.isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        if (isVerificationSendLimited("change-password", servletRequest)) {
            return Result.error(429, SecurityRateLimitService.RATE_LIMIT_MESSAGE);
        }
        try {
            verificationCodeService.sendCode("change-password", email, email);
            return Result.success("验证码已发送至您的邮箱");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public Result<String> changePassword(@RequestBody Map<String, Object> request, HttpServletRequest servletRequest) {
        String oldPassword = stringValue(request.get("oldPassword"));
        String newPassword = stringValue(request.get("newPassword"));
        String code = stringValue(request.get("code"));

        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error("请输入当前密码");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("新密码长度至少 6 位");
        }
        if (code == null || code.isEmpty()) {
            return Result.error("请输入验证码");
        }

        User currentUser = currentUser();
        if (currentUser == null) {
            return Result.error(401, "请先登录");
        }
        User user = loginMapper.selectById(currentUser.getId());
        if (user == null) {
            return Result.error("用户不存在，请重新登录");
        }
        if (!sensitiveDataEncryptor.matches(oldPassword, user.getPassword())) {
            return Result.error("当前密码不正确");
        }

        // 验证验证码
        try {
            if (!verificationCodeService.verifyCode("change-password", user.getEmail(), code, resolveClientIp(servletRequest))) {
                return Result.error("验证码错误或已过期");
            }
        } catch (RateLimitExceededException e) {
            return Result.error(429, e.getMessage());
        }

        String encryptedPassword = sensitiveDataEncryptor.encrypt(newPassword);
        int updated = resetPasswordMapper.resetById(user.getId(), encryptedPassword);
        if (updated <= 0) {
            return Result.error("密码修改失败");
        }
        return Result.success("密码修改成功");
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private boolean isVerificationSendLimited(String scene, HttpServletRequest request) {
        return rateLimitService != null
                && rateLimitService.consumeVerificationSend(scene, resolveClientIp(request));
    }

    private String resolveClientIp(HttpServletRequest request) {
        return clientIpResolver == null ? "unknown" : clientIpResolver.resolve(request);
    }
}
