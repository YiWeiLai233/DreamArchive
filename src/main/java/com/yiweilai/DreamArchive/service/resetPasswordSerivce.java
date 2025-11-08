package com.yiweilai.DreamArchive.service;

import org.springframework.stereotype.Service;

@Service
public class resetPasswordSerivce {
    //可以通过正则表达来判断是账号还是email 通过判断是否有@这个字符
    public String resetPassword(String email) {
        if (email.matches(".[@]]"));
        return "200";
    }
}
