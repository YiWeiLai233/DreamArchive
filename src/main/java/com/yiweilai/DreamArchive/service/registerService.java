package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.registerMapper;
import com.yiweilai.DreamArchive.util.CaptchaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
//写入用户表
public class registerService {
    @Autowired
    registerMapper registerMapper;
    @Autowired
    CaptchaGenerator generator;
    @Transactional  // 开启事务确保数据库操作出错时会自动回滚
    public String newUser(String username,String password,String email){
        //判断账户是否存在
        User exitsEmail = registerMapper.findByEmail(email);
        User exitsName = registerMapper.findByUsername(username);
        if(exitsEmail != null|| exitsName != null){
            return "用户已经存在";
        }
    User user = new User();
    //设置一个新用户并且存到Mysql数据库中
    user.setUsername(username);
    user.setPassword(password);
    user.setEmail(email);
    int result = registerMapper.newUser(user);
        if (result > 0) {
            return "200"; // 注册成功
        } else {
            return "500"; // 数据库插入失败
        }
    }
}
