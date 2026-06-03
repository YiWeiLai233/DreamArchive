package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.service.MinioService;
import com.yiweilai.DreamArchive.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private LoginMapper loginMapper;

    @Autowired
    private MinioService minioService;

    @GetMapping("/me")
    public Result<User> me() {
        User currentUser = currentUser();
        if (currentUser == null) {
            return Result.error(401, "请先登录");
        }
        User user = loginMapper.selectById(currentUser.getId());
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            return Result.error(401, "登录用户不存在");
        }
        user.setPassword(null);
        enrichAvatarUrl(user);
        return Result.success(user);
    }

    /**
     * 通过邮箱获取用户信息
     * GET /api/user/by-email?email=xxx
     */
    @GetMapping("/by-email")
    public Result<User> getUserByEmail(@RequestParam String email) {
        try {
            User user = loginMapper.selectByEmail(email);
            if (user == null) {
                return Result.error("用户不存在");
            }
            // 清空密码再返回
            user.setPassword(null);
            if (!canAccessUser(user)) {
                return Result.error(403, "\u6743\u9650\u4e0d\u8db3");
            }
            enrichAvatarUrl(user);
            return Result.success(user);
        } catch (Exception e) {
            log.error("查询用户失败", e);
            return Result.error("查询失败，请稍后重试");
        }
    }

    /**
     * 通过用户名获取用户信息
     * GET /api/user/by-username?username=xxx
     */
    @GetMapping("/by-username")
    public Result<User> getUserByUsername(@RequestParam String username) {
        try {
            User user = loginMapper.selectByUsername(username);
            if (user == null) {
                return Result.error("用户不存在");
            }
            // 清空密码再返回
            user.setPassword(null);
            if (!canAccessUser(user)) {
                return Result.error(403, "\u6743\u9650\u4e0d\u8db3");
            }
            enrichAvatarUrl(user);
            return Result.success(user);
        } catch (Exception e) {
            log.error("查询用户失败", e);
            return Result.error("查询失败，请稍后重试");
        }
    }
    @PostMapping("/avatar")
    public Result<String> updateAvatar(@RequestBody Map<String, String> request) {
        try {
            User currentUser = currentUser();
            if (currentUser == null) {
                return Result.error(401, "请先登录");
            }
            String avatarUrl = request.get("avatarUrl");
            loginMapper.updateAvatarUrl(currentUser.getId(), avatarUrl);
            return Result.success("头像已更新");
        } catch (Exception e) {
            log.error("更新头像失败", e);
            return Result.error("更新失败，请稍后重试");
        }
    }

    private void enrichAvatarUrl(User user) {
        if (user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            String objectName = minioService.extractObjectName(user.getAvatarUrl());
            String url = minioService.getPresignedUrl(objectName);
            if (url != null) {
                user.setAvatarUrl(url);
            }
        }
    }

    private boolean canAccessUser(User user) {
        User currentUser = currentUser();
        return currentUser != null
                && user != null
                && ("ADMIN".equalsIgnoreCase(currentUser.getRole()) || currentUser.getId() == user.getId());
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        return user;
    }
}
