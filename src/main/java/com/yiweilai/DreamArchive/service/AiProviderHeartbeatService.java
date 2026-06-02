package com.yiweilai.DreamArchive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.DreamArchive.DTO.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiProviderHeartbeatService {

    private static final Logger log = LoggerFactory.getLogger(AiProviderHeartbeatService.class);
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final AiProviderPool providerPool;
    private final boolean heartbeatEnabled;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AiProviderHeartbeatService(
            AiProviderPool providerPool,
            @Value("${ai.pool.heartbeat.enabled:true}") boolean heartbeatEnabled,
            @Value("${ai.pool.heartbeat.timeout-seconds:10}") long timeoutSeconds) {
        this(
                providerPool,
                heartbeatEnabled,
                HttpClient.newBuilder().connectTimeout(DEFAULT_CONNECT_TIMEOUT).build(),
                Duration.ofSeconds(timeoutSeconds)
        );
    }

    AiProviderHeartbeatService(
            AiProviderPool providerPool,
            boolean heartbeatEnabled,
            HttpClient httpClient,
            Duration requestTimeout) {
        this.providerPool = providerPool;
        this.heartbeatEnabled = heartbeatEnabled;
        this.httpClient = httpClient;
        this.requestTimeout = requestTimeout;
    }

    @Async("taskExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpAfterStartup() {
        checkAllProviders();
    }

    void checkAllProviders() {
        if (!heartbeatEnabled) {
            log.info("AI provider heartbeat skipped: disabled by config");
            return;
        }

        List<AiProvider> providers = providerPool.getProviders();
        if (providers.isEmpty()) {
            log.warn("AI provider heartbeat skipped: no providers configured");
            return;
        }

        for (AiProvider provider : providers) {
            if (!provider.isEnabled()) {
                log.info("AI provider [{}] heartbeat skipped: disabled", provider.getName());
                continue;
            }
            checkProvider(provider);
        }
    }

    private void checkProvider(AiProvider provider) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeUrl(provider.getUrl())))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + provider.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(provider)))
                    .build();

            long startMs = System.currentTimeMillis();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsedMs = Math.max(1, System.currentTimeMillis() - startMs);
            providerPool.reportLatency(provider, elapsedMs);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                providerPool.reportSuccess(provider);
                log.info("AI provider [{}] heartbeat OK, latency: {}ms", provider.getName(), elapsedMs);
                return;
            }

            RuntimeException failure = new RuntimeException("heartbeat returned status " + response.statusCode());
            providerPool.reportFailure(provider, failure);
            log.warn("AI provider [{}] heartbeat failed with status {}, latency: {}ms",
                    provider.getName(), response.statusCode(), elapsedMs);
        } catch (Exception e) {
            providerPool.reportFailure(provider, e);
            log.warn("AI provider [{}] heartbeat failed: {}", provider.getName(), e.getMessage());
        }
    }

    private String buildRequestBody(AiProvider provider) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", provider.getModel());
        body.put("temperature", 0);
        body.put("max_tokens", 1);
        body.put("messages", List.of(Map.of("role", "user", "content", "ping")));
        return objectMapper.writeValueAsString(body);
    }

    private String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalStateException("AI provider url is not configured");
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
}
