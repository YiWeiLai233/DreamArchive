package com.yiweilai.DreamArchive.util;

import java.util.Random;
import org.springframework.stereotype.Component;
@Component
public class CaptchaGenerator {
    public String generateCaptcha() {
        Random rand = new Random();
        double captcha =rand.nextDouble()*10000;
        return String.valueOf(1000 + rand.nextInt(9000));
    }
}
