package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.mapper.ResetPasswordMapper;
import com.yiweilai.DreamArchive.util.Result;
import com.yiweilai.DreamArchive.util.passwordEncrypt;
import org.springframework.beans.factory.annotation.Autowired;
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
    private passwordEncrypt passwordEncrypt;

    @PostMapping("/change-password")
    public Result<String> changePassword(@RequestBody Map<String, Object> request) {
        String oldPassword = stringValue(request.get("oldPassword"));
        String newPassword = stringValue(request.get("newPassword"));
        String username = stringValue(request.get("username"));
        String email = stringValue(request.get("email"));
        Integer userId = intValue(request.get("userId"));

        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error("请输入当前密码");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("新密码长度至少 6 位");
        }

        User user = findUser(userId, username, email);
        if (user == null) {
            return Result.error("用户不存在，请重新登录");
        }
        if (!passwordEncrypt.matches(oldPassword, user.getPassword())) {
            return Result.error("当前密码不正确");
        }

        String encryptedPassword = passwordEncrypt.encrypt(newPassword);
        int updated = resetPasswordMapper.resetById(user.getId(), encryptedPassword);
        if (updated <= 0) {
            return Result.error("密码修改失败");
        }
        return Result.success("密码修改成功");
    }

    private User findUser(Integer userId, String username, String email) {
        User user = null;
        if (userId != null) {
            user = loginMapper.selectById(userId);
        }
        if (user == null && username != null && !username.isEmpty()) {
            user = loginMapper.selectByUsername(username);
        }
        if (user == null && email != null && !email.isEmpty()) {
            user = loginMapper.selectByEmail(email);
        }
        return user;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? null : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
