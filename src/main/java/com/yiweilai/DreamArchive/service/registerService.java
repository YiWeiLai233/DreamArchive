package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.mapper.registerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
//写入用户表
public class registerService {
    @Autowired
    registerMapper registerMapper;
    public String newUser(String username,String password,String email){

    }
}
