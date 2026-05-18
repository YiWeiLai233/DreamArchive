package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.RegisterMapper;
import com.yiweilai.DreamArchive.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterService {
    @Autowired
    private RegisterMapper registerMapper;

    @Transactional
    public Result<User> newUser(String username, String password, String email) {
        // 判断账户是否存在
        User existEmail = registerMapper.findByEmail(email);
        User existName = registerMapper.findByUsername(username);

        if (existEmail != null) {
            return Result.error("该邮箱已被注册");
        }
        if (existName != null) {
            return Result.error("该用户名已被使用");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);

        int result = registerMapper.newUser(user);
        if (result > 0) {
            return Result.success("注册成功", user);
        } else {
            return Result.error("注册失败，请稍后重试");
        }
    }
}
