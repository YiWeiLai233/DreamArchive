package com.yiweilai.DreamArchive.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.DreamArchive.DTO.messages;
import com.yiweilai.DreamArchive.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

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

    public String analyzeDream(String content) {
        List<messages> requestMessages = new ArrayList<>();
        requestMessages.add(new messages(
                "system",
                "你是一名温和、专业的梦境分析助手。请用中文从情绪、象征和现实启发三个角度分析梦境，避免绝对化判断。"
        ));
        requestMessages.add(new messages("user", content));

        String json = JsonUtil.toJSON(requestMessages, model);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeUrl(url)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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
