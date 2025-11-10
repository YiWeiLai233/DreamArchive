package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.User;
import com.yiweilai.DreamArchive.mapper.registerMapper;
import com.yiweilai.DreamArchive.util.CaptchaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
//写入用户表
public class registerService {
    @Autowired
    registerMapper registerMapper;
    @Autowired
    CaptchaGenerator generator;
    public String newUser(String username,String password,String email){
    User user = new User();
    //设置一个新用户并且存到Mysql数据库中
    user.setUsername(username);
    user.setPassword(password);
    user.setEmail(email);
        return "200";
    }
}
