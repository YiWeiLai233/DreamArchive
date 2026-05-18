package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.util.CaptchaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
//怎么样做到每次自动刷新刷新验证码 然后要存储到哪里呢 而且写一次就要重新计算一次图片吗
//redis存储验证码
public class captcha {


    public void captchaPicture(){
        CaptchaGenerator generator =new CaptchaGenerator();
        generator.generateCaptcha();
    }
}
