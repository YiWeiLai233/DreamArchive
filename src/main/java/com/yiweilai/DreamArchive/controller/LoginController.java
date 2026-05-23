package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.LoginResponse;
import com.yiweilai.DreamArchive.service.LoginService;
import com.yiweilai.DreamArchive.service.VerificationCodeService;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        return loginService.login(username, password);
    }

    @PostMapping("/login/send-code")
    public Result<String> sendCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return Result.error("请输入邮箱");
        }
        try {
            verificationCodeService.sendCode("login", email, email);
            return Result.success("验证码已发送至您的邮箱");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login/code")
    public Result<LoginResponse> loginByCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        if (email == null || email.isEmpty()) {
            return Result.error("请输入邮箱");
        }
        if (code == null || code.isEmpty()) {
            return Result.error("请输入验证码");
        }
        if (!verificationCodeService.verifyCode("login", email, code)) {
            return Result.error("验证码错误或已过期");
        }
        return loginService.loginByEmail(email);
    }

    @PostMapping("/account/setup")
    public Result<String> setupAccount(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        if (username == null || username.trim().length() < 3) {
            return Result.error("用户名至少需要3个字符");
        }
        if (password == null || password.length() < 6) {
            return Result.error("密码长度至少6位");
        }
        return loginService.setupAccount(username.trim(), password);
    }
}
