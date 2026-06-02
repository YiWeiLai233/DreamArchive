package com.yiweilai.DreamArchive.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class VerificationCodeService {

    private static final int CODE_TTL_MINUTES = 5;
    private static final int RATE_LIMIT_SECONDS = 60;
    private static final Logger log = LoggerFactory.getLogger(VerificationCodeService.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JavaMailSender mailSender;

    private final SecureRandom random = new SecureRandom();

    public void sendCode(String scene, String identifier, String email) {
        String rateKey = "rate:" + scene + ":" + identifier;
        String codeKey = "code:" + scene + ":" + identifier;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateKey))) {
            throw new RuntimeException("验证码发送过于频繁，请1分钟后再试");
        }

        String code = String.format("%06d", random.nextInt(1000000));

        String subject;
        String prefix;
        switch (scene) {
            case "register":
                subject = "梦境档案 - 注册验证码";
                prefix = "您正在注册梦境档案账号，验证码是：";
                break;
            case "change-password":
                subject = "梦境档案 - 修改密码验证码";
                prefix = "您正在修改密码，验证码是：";
                break;
            case "login":
                subject = "梦境档案 - 登录验证码";
                prefix = "您正在登录梦境档案，验证码是：";
                break;
            default:
                subject = "梦境档案 - 密码重置验证码";
                prefix = "您正在重置密码，验证码是：";
                break;
        }

        // 先保存验证码，确保用户收到后一定可以完成校验。
        redisTemplate.opsForValue().set(codeKey, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);

        try {
            sendEmail(email, subject, prefix + code);
        } catch (Exception e) {
            redisTemplate.delete(codeKey);
            throw new RuntimeException("邮件发送失败，请检查邮箱地址是否正确");
        }

        try {
            redisTemplate.opsForValue().set(rateKey, "1", RATE_LIMIT_SECONDS, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            log.warn("验证码已发送，但写入发送频率限制失败: scene={}, identifier={}", scene, identifier, e);
        }
    }

    public boolean verifyCode(String scene, String identifier, String code) {
        String codeKey = "code:" + scene + ":" + identifier;
        String stored = redisTemplate.opsForValue().get(codeKey);
        if (stored == null) {
            return false;
        }
        if (stored.equals(code)) {
            redisTemplate.delete(codeKey);
            return true;
        }
        return false;
    }

    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("859399899@qq.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text + "\n\n验证码有效期为5分钟，请勿泄露给他人。\n\n如非本人操作，请忽略此邮件。");
        mailSender.send(message);
    }
}
