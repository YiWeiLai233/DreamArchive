package com.yiweilai.DreamArchive.controller;

import com.yiweilai.DreamArchive.service.AiService;
import com.yiweilai.DreamArchive.service.ClientIpResolver;
import com.yiweilai.DreamArchive.service.SecurityRateLimitService;
import com.yiweilai.DreamArchive.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/guest")
public class GuestController {

    private static final Logger log = LoggerFactory.getLogger(GuestController.class);
    private static final String GUEST_ANALYZE_KEY_PREFIX = "guest:analyze:";
    private static final int MAX_GUEST_ANALYZES = 3;

    @Autowired
    private AiService aiService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SecurityRateLimitService rateLimitService;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @PostMapping("/analyze")
    public Result<Map<String, String>> guestAnalyze(@RequestBody Map<String, Object> request, HttpServletRequest servletRequest) {
        String deviceId = toStringValue(request.get("deviceId"));
        String content = toStringValue(request.get("content"));

        if (deviceId.isBlank()) {
            return Result.error("设备标识缺失");
        }
        if (content.isBlank()) {
            return Result.error("梦境内容不能为空");
        }
        if (rateLimitService != null
                && rateLimitService.consumeGuestAnalyzeIp(resolveClientIp(servletRequest))) {
            return Result.error(429, SecurityRateLimitService.RATE_LIMIT_MESSAGE);
        }

        String redisKey = GUEST_ANALYZE_KEY_PREFIX + deviceId;
        String countStr = redisTemplate.opsForValue().get(redisKey);
        int currentCount = countStr == null ? 0 : Integer.parseInt(countStr);

        if (currentCount >= MAX_GUEST_ANALYZES) {
            return Result.error(403, "游客体验已用完，请注册登录后继续使用");
        }

        try {
            String interpretation = aiService.analyzeDream(content);
            redisTemplate.opsForValue().set(redisKey, String.valueOf(currentCount + 1), 365, TimeUnit.DAYS);
            return Result.success(Map.of("interpretation", interpretation));
        } catch (Exception e) {
            log.error("游客 AI 解析失败", e);
            return Result.error("AI 解析失败，请稍后重试");
        }
    }

    private String toStringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String resolveClientIp(HttpServletRequest request) {
        return clientIpResolver == null ? "unknown" : clientIpResolver.resolve(request);
    }
}
