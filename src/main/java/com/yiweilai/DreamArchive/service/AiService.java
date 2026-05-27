package com.yiweilai.DreamArchive.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.DreamArchive.DTO.AiProvider;
import com.yiweilai.DreamArchive.DTO.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;;

@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    @Autowired
    private AiProviderPool providerPool;

    @Autowired
    private MinioService minioService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public String analyzeDream(String content) {
        return analyzeDream(content, null, null, null, null);
    }

    public String analyzeDream(String content, String imageUrl) {
        return analyzeDream(content, imageUrl, null, null, null);
    }

    public String analyzeDream(String content, String imageUrl, String emotion, String place, String time) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("梦境内容不能为空");
        }

        boolean hasImage = imageUrl != null && !imageUrl.isBlank();

        if (hasImage) {
            try {
                return analyzeDreamWithImage(content, imageUrl, emotion, place, time);
            } catch (Exception e) {
                log.warn("图片分析失败，降级为纯文字分析: {}", e.getMessage());
            }
        }

        // 纯文字分析（无图片 or 图片分析失败降级）
        String userMessage = buildUserMessage(content, emotion, place, time);
        List<Message> requestMessages = new ArrayList<>();
        requestMessages.add(new Message(
                "system",
                "你是一名温和、专业的梦境分析助手。请用中文按[整体解读、情绪层面、象征层面、现实启发]四个小标题分析梦境，避免绝对化判断，不要使用 ** 等 Markdown 符号。"
        ));
        requestMessages.add(new Message("user", userMessage));
        return callAi(requestMessages, 0.3, 30, 2000);
    }

    private String analyzeDreamWithImage(String content, String imageUrl, String emotion, String place, String time) {
        String objectName = minioService.extractObjectName(imageUrl);
        String base64Url = minioService.getImageAsBase64(objectName);
        if (base64Url == null) {
            throw new RuntimeException("无法下载图片进行分析");
        }

        String textContent = buildUserMessage(content, emotion, place, time);
        List<Message> requestMessages = new ArrayList<>();
        requestMessages.add(new Message(
                "system",
                "你是一名温和、专业的梦境分析助手。用户会提供一幅手绘梦境画和文字描述。请综合图片中的画面元素（场景、人物、色彩、构图）和文字描述来分析梦境。用中文按[整体解读、情绪层面、象征层面、现实启发]四个小标题分析，避免绝对化判断，不要使用 ** 等 Markdown 符号。"
        ));

        List<Map<String, Object>> contentParts = new ArrayList<>();
        contentParts.add(Map.of("type", "text", "text", textContent));
        contentParts.add(Map.of("type", "image_url", "image_url", Map.of("url", base64Url)));
        requestMessages.add(new Message("user", contentParts));

        return callAi(requestMessages, 0.3, 60, 2000, providerPool.getVisionProvider());
    }

    private String buildUserMessage(String content, String emotion, String place, String time) {
        StringBuilder sb = new StringBuilder(content);
        if (emotion != null && !emotion.isBlank()) {
            sb.append("\n\n").append("心情：").append(emotion);
        }
        if (place != null && !place.isBlank()) {
            sb.append("\n").append("地点：").append(place);
        }
        if (time != null && !time.isBlank()) {
            sb.append("\n").append("时间：").append(time);
        }
        return sb.toString();
    }

    public String generateDreamTitle(String content) {
        if (content == null || content.isBlank()) {
            return "未命名梦境";
        }

        List<Message> requestMessages = new ArrayList<>();
        requestMessages.add(new Message(
                "system",
                "你是梦境标题助手。请根据梦境内容生成一个温柔、简短、有画面感的中文标题。要求：不超过 12 个中文字符；只返回标题本身；不要解释；不要引号、书名号或标点。"
        ));
        requestMessages.add(new Message("user", content));

        try {
            return cleanGeneratedTitle(callAi(requestMessages, 0.4, 45), content);
        } catch (Exception e) {
            log.warn("AI dream title generation failed, using fallback title", e);
            return fallbackTitle(content);
        }
    }

    private String callAi(List<Message> messages, double temperature, int timeoutSeconds) {
        return callAi(messages, temperature, timeoutSeconds, null);
    }

    private String callAi(List<Message> messages, double temperature, int timeoutSeconds, Integer maxTokens) {
        return callAi(messages, temperature, timeoutSeconds, maxTokens, null);
    }

    private String callAi(List<Message> messages, double temperature, int timeoutSeconds, Integer maxTokens, String providerName) {
        AiProvider provider = providerName != null ? providerPool.acquire(providerName) : providerPool.acquire();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", provider.getModel());
            body.put("temperature", temperature);
            body.put("messages", messages);
            if (maxTokens != null) {
                body.put("max_tokens", maxTokens);
            }
            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeUrl(provider.getUrl())))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + provider.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            long startMs = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsedMs = System.currentTimeMillis() - startMs;
            providerPool.reportLatency(provider, elapsedMs);
            log.info("AI [{}] response status: {}, latency: {}ms", provider.getName(), response.statusCode(), elapsedMs);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("AI 接口返回异常: " + response.statusCode());
            }
            providerPool.reportSuccess(provider);
            return extractContent(response.body());
        } catch (Exception e) {
            providerPool.reportFailure(provider, e);
            throw new RuntimeException("AI 解析失败: " + e.getMessage(), e);
        }
    }

    private String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalStateException("AI 接口地址未配置");
        }
        String trimmed = rawUrl.trim();
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed;
        }
        if (trimmed.endsWith("/v1")) {
            return trimmed + "/chat/completions";
        }
        if (trimmed.endsWith("/v1/")) {
            return trimmed + "chat/completions";
        }
        if (trimmed.endsWith("/")) {
            return trimmed + "v1/chat/completions";
        }
        return trimmed + "/v1/chat/completions";
    }

    private String cleanGeneratedTitle(String title, String content) {
        String cleaned = title == null ? "" : title
                .replace("**", "")
                .replace("标题：", "")
                .replace("标题:", "")
                .replace("《", "")
                .replace("》", "")
                .replace("\"", "")
                .replace("'", "")
                .replaceAll("[\\r\\n]+", " ")
                .trim();

        if (cleaned.isBlank()) {
            return fallbackTitle(content);
        }
        return cleaned.length() > 16 ? cleaned.substring(0, 16) : cleaned;
    }

    private String fallbackTitle(String content) {
        String compact = content == null ? "" : content.replaceAll("\\s+", "").trim();
        if (compact.isBlank()) {
            return "未命名梦境";
        }
        return compact.length() > 12 ? compact.substring(0, 12) : compact;
    }

    private String extractContent(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (!content.isMissingNode() && !content.asText().isBlank()) {
            return content.asText();
        }
        return body;
    }
}
