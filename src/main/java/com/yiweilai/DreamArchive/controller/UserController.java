package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private LoginMapper loginMapper;

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
            return Result.success(user);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
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
            return Result.success(user);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
