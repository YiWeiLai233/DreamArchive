package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.LoginMapper;
import com.yiweilai.DreamArchive.util.Result;
import com.yiweilai.DreamArchive.util.passwordEncrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    @Autowired
    private LoginMapper loginMapper;

    @Autowired
    private passwordEncrypt passwordEncrypt;

    public Result<User> login(String username, String password) {
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

        // 验证密码
        if (!passwordEncrypt.matches(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        // 登录成功，返回用户信息（不包含密码）
        user.setPassword(null);
        return Result.success("登录成功", user);
    }
}
