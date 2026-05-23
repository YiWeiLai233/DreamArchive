package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.LoginResponse;
import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.service.RegisterService;
import com.yiweilai.DreamArchive.service.TokenService;
import com.yiweilai.DreamArchive.service.VerificationCodeService;
import com.yiweilai.DreamArchive.util.PasswordEncrypt;
import com.yiweilai.DreamArchive.util.Result;
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
    private PasswordEncrypt passwordEncrypt;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/register/send-code")
    public Result<String> sendCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return Result.error("请输入邮箱");
        }
        try {
            verificationCodeService.sendCode("register", email, email);
            return Result.success("验证码已发送至您的邮箱");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody User user, @RequestParam String code) {
        if (code.isEmpty()) {
            return Result.error("请输入验证码");
        }
        if (!verificationCodeService.verifyCode("register", user.getEmail(), code)) {
            return Result.error("验证码错误或已过期");
        }
        String encryptedPassword = passwordEncrypt.encrypt(user.getPassword());
        Result<User> result = registerService.newUser(user.getUsername(), encryptedPassword, user.getEmail());
        if (result.getCode() != 200) {
            return Result.error(result.getMessage());
        }
        User registered = result.getData();
        String token = tokenService.generateToken(registered);
        registered.setPassword(null);
        return Result.success("注册成功", new LoginResponse(registered, token));
    }
}
