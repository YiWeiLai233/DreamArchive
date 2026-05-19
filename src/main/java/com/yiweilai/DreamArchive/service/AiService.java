package com.yiweilai.DreamArchive.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.DreamArchive.DTO.messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    @Value("${ai.api.url}")
    private String url;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public String analyzeDream(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("梦境内容不能为空");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI API Key 未配置");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("AI 模型未配置");
        }

        List<messages> requestMessages = new ArrayList<>();
        requestMessages.add(new messages(
                "system",
                "你是一名温和、专业的梦境分析助手。请用中文按“整体解读、情绪层面、象征层面、现实启发”四个小标题分析梦境，避免绝对化判断，不要使用 ** 等 Markdown 符号。"
        ));
        requestMessages.add(new messages("user", content));

        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0.5,
                    "messages", requestMessages
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeUrl(url)))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("AI response status: {}", response.statusCode());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("AI 接口返回异常: " + response.statusCode());
            }
            return extractContent(response.body());
        } catch (Exception e) {
            log.error("AI dream analysis failed", e);
            throw new RuntimeException("AI 解析失败: " + e.getMessage(), e);
        }
    }

    public String aiSerice(String content) {
        return analyzeDream(content);
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

    private String extractContent(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (!content.isMissingNode() && !content.asText().isBlank()) {
            return content.asText();
        }
        return body;
    }
}
