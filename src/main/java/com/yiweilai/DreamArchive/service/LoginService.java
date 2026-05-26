package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.LoginResponse;
import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.mapper.RegisterMapper;
import com.yiweilai.DreamArchive.util.PasswordEncrypt;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LoginService {

    @Autowired
    private LoginMapper loginMapper;

    @Autowired
    private RegisterMapper registerMapper;

    @Autowired
    private PasswordEncrypt passwordEncrypt;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MinioService minioService;

    private void enrichAvatarUrl(User user) {
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            String objectName = minioService.extractObjectName(user.getAvatarUrl());
            String url = minioService.getPresignedUrl(objectName);
            if (url != null) {
                user.setAvatarUrl(url);
            }
        }
    }

    public Result<LoginResponse> login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.isEmpty()) {
            return Result.error("密码不能为空");
        }

        // 根据用户名或邮箱查询用户
        User user = loginMapper.login(username.trim(), username.trim());
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (Boolean.TRUE.equals(user.getDeleted())) {
            return Result.error(403, "账号已被删除，无法登录");
        }
        if ("BANNED".equalsIgnoreCase(user.getStatus())) {
            return Result.error(403, "账号已被封禁，无法登录");
        }

        // 验证密码
        if (!passwordEncrypt.matches(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        // 登录成功，返回用户信息（不包含密码）
        String token = tokenService.generateToken(user);
        user.setPassword(null);
        enrichAvatarUrl(user);
        return Result.success("登录成功", new LoginResponse(user, token));
    }

    public Result<LoginResponse> loginByEmail(String email) {
        User user = loginMapper.selectByEmail(email);

        if (user == null) {
            // 自动注册
            String tempUsername = "用户" + UUID.randomUUID().toString().substring(0, 6);
            String tempPassword = passwordEncrypt.encrypt(UUID.randomUUID().toString());
            User newUser = new User();
            newUser.setUsername(tempUsername);
            newUser.setEmail(email);
            newUser.setPassword(tempPassword);
            // 第一个注册的用户自动成为超级管理员
            if (registerMapper.countUsers() == 0) {
                newUser.setRole("SUPER_ADMIN");
            }
            registerMapper.newUser(newUser);
            // 查询刚创建的用户（获取自增ID）
            user = loginMapper.selectByEmail(email);
            String token = tokenService.generateToken(user);
            user.setPassword(null);
            enrichAvatarUrl(user);
            return Result.success("注册成功，请设置用户名和密码", new LoginResponse(user, token, true));
        }

        if (Boolean.TRUE.equals(user.getDeleted())) {
            return Result.error(403, "账号已被删除，无法登录");
        }
        if ("BANNED".equalsIgnoreCase(user.getStatus())) {
            return Result.error(403, "账号已被封禁，无法登录");
        }
        String token = tokenService.generateToken(user);
        user.setPassword(null);
        enrichAvatarUrl(user);
        return Result.success("登录成功", new LoginResponse(user, token));
    }

    public Result<String> setupAccount(String username, String password) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (currentUser == null) {
            return Result.error("请先登录");
        }

        // 检查用户名是否已被占用
        User existing = registerMapper.findByUsername(username);
        if (existing != null && existing.getId() != currentUser.getId()) {
            return Result.error("该用户名已被使用");
        }

        String encryptedPassword = passwordEncrypt.encrypt(password);
        int rows = registerMapper.updateUsernameAndPassword(currentUser.getId(), username, encryptedPassword);
        if (rows > 0) {
            return Result.success("设置成功");
        }
        return Result.error("设置失败");
    }
}
