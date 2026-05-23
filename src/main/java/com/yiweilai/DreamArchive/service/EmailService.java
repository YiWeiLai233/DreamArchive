package com.yiweilai.DreamArchive.service;

import org.springframework.stereotype.Service;

@Service
//连接imap服务发送验证码邮件到用户手上
public class EmailService {
    public String emailCode() {
        return "access";
    }
}
