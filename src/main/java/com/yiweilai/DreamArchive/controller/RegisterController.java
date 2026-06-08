package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.LoginResponse;
import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.service.AuthCookieService;
import com.yiweilai.DreamArchive.service.ClientIpResolver;
import com.yiweilai.DreamArchive.service.CsrfTokenService;
import com.yiweilai.DreamArchive.service.RateLimitExceededException;
import com.yiweilai.DreamArchive.service.RegisterService;
import com.yiweilai.DreamArchive.service.SecurityRateLimitService;
import com.yiweilai.DreamArchive.service.TokenService;
import com.yiweilai.DreamArchive.service.VerificationCodeService;
import com.yiweilai.DreamArchive.util.Result;
import com.yiweilai.DreamArchive.util.SensitiveDataEncryptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class RegisterController {

    @Autowired
    private RegisterService registerService;

    @Autowired
    private SensitiveDataEncryptor sensitiveDataEncryptor;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthCookieService authCookieService;

    @Autowired
    private CsrfTokenService csrfTokenService;

    @Autowired
    private SecurityRateLimitService rateLimitService;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @PostMapping("/register/send-code")
    public Result<String> sendCode(@RequestBody Map<String, String> request, HttpServletRequest servletRequest) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return Result.error("请输入邮箱");
        }
        if (isVerificationSendLimited("register", servletRequest)) {
            return Result.error(429, SecurityRateLimitService.RATE_LIMIT_MESSAGE);
        }
        try {
            verificationCodeService.sendCode("register", email, email);
            return Result.success("验证码已发送至您的邮箱");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody User user,
                                          @RequestParam String code,
                                          HttpServletResponse response,
                                          HttpServletRequest servletRequest) {
        if (code.isEmpty()) {
            return Result.error("请输入验证码");
        }
        try {
            if (!verificationCodeService.verifyCode("register", user.getEmail(), code, resolveClientIp(servletRequest))) {
                return Result.error("验证码错误或已过期");
            }
        } catch (RateLimitExceededException e) {
            return Result.error(429, e.getMessage());
        }
        String encryptedPassword = sensitiveDataEncryptor.encrypt(user.getPassword());
        Result<User> result = registerService.newUser(user.getUsername(), encryptedPassword, user.getEmail());
        if (result.getCode() != 200) {
            return Result.error(result.getMessage());
        }
        User registered = result.getData();
        String token = tokenService.generateToken(registered);
        authCookieService.addAuthCookie(response, token);
        authCookieService.addCsrfCookie(response, csrfTokenService.generateToken());
        registered.setPassword(null);
        return Result.success("注册成功", new LoginResponse(registered, null));
    }

    private boolean isVerificationSendLimited(String scene, HttpServletRequest request) {
        return rateLimitService != null
                && rateLimitService.consumeVerificationSend(scene, resolveClientIp(request));
    }

    private String resolveClientIp(HttpServletRequest request) {
        return clientIpResolver == null ? "unknown" : clientIpResolver.resolve(request);
    }
}
