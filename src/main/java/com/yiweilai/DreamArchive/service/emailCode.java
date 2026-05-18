package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.util.CaptchaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service



//连接imap服务发送验证码邮件到用户手上
public class emailCode {
    @Autowired
    CaptchaGenerator generator;
    public String emailCode() {



        return "access";
    }
}
