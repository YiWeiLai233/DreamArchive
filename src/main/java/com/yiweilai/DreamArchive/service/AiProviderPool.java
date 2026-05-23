package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConfigurationProperties(prefix = "ai.pool")
public class AiProviderPool {

    private static final Logger log = LoggerFactory.getLogger(AiProviderPool.class);

    private static final int MAX_FAIL_COUNT = 3;
    private static final long CIRCUIT_OPEN_DURATION_MS = 60_000;

    private List<AiProvider> providers = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> currentWeight = new ConcurrentHashMap<>();

    public List<AiProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }

    public void setProviders(List<AiProvider> providers) {
        this.providers = providers != null ? new CopyOnWriteArrayList<>(providers) : new CopyOnWriteArrayList<>();
    }

    @PostConstruct
    public void init() {
        if (providers.isEmpty()) {
            log.warn("AI pool has no providers. Add ai.pool.providers[0].name/... in application.properties");
        } else {
            for (AiProvider p : providers) {
                currentWeight.put(p.getName(), 0);
                log.info("AI provider loaded: {} (weight={}, enabled={})", p.getName(), p.getWeight(), p.isEnabled());
            }
        }
    }

    public synchronized AiProvider acquire() {
        List<AiProvider> available = getAvailableProviders();
        if (available.isEmpty()) {
            available = getHalfOpenProviders();
            if (available.isEmpty()) {
                throw new RuntimeException("All AI providers are unavailable");
            }
        }
        return smoothWeightedSelect(available);
    }

    public void reportSuccess(AiProvider provider) {
        if (provider == null) return;
        provider.setFailCount(0);
        provider.setCircuitOpen(false);
    }

    public void reportFailure(AiProvider provider, Exception e) {
        if (provider == null) return;
        int count = provider.getFailCount() + 1;
        provider.setFailCount(count);
        provider.setLastFailTime(System.currentTimeMillis());
        if (count >= MAX_FAIL_COUNT) {
            provider.setCircuitOpen(true);
            log.warn("AI provider [{}] failed {} times, circuit open for {}s", provider.getName(), count, CIRCUIT_OPEN_DURATION_MS / 1000);
        } else {
            log.warn("AI provider [{}] failed ({}/{}): {}", provider.getName(), count, MAX_FAIL_COUNT, e.getMessage());
        }
    }

    /**
     * 报告延迟，用指数移动平均更新
     */
    public void reportLatency(AiProvider provider, long latencyMs) {
        if (provider == null) return;
        double old = provider.getAvgLatencyMs();
        if (old == 0) {
            provider.setAvgLatencyMs(latencyMs);
        } else {
            // EMA: alpha=0.3, 越新的请求权重越大
            provider.setAvgLatencyMs(old * 0.7 + latencyMs * 0.3);
        }
    }

    public void addProvider(AiProvider provider) {
        validateProvider(provider);
        for (AiProvider p : providers) {
            if (p.getName().equals(provider.getName())) {
                throw new IllegalArgumentException("provider [" + provider.getName() + "] already exists");
            }
        }
        currentWeight.put(provider.getName(), 0);
        providers.add(provider);
    }

    public boolean removeProvider(String name) {
        boolean removed = providers.removeIf(p -> p.getName().equals(name));
        if (removed) currentWeight.remove(name);
        return removed;
    }

    public AiProvider updateProvider(String name, Integer weight, Boolean enabled) {
        for (AiProvider p : providers) {
            if (p.getName().equals(name)) {
                if (weight != null) {
                    if (weight <= 0) {
                        throw new IllegalArgumentException("weight must be greater than 0");
                    }
                    p.setWeight(weight);
                }
                if (enabled != null) p.setEnabled(enabled);
                return p;
            }
        }
        return null;
    }

    public boolean resetCircuit(String name) {
        for (AiProvider p : providers) {
            if (p.getName().equals(name)) {
                p.setFailCount(0);
                p.setCircuitOpen(false);
                return true;
            }
        }
        return false;
    }

    private void validateProvider(AiProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider is required");
        }
        if (isBlank(provider.getName())) {
            throw new IllegalArgumentException("provider name is required");
        }
        if (isBlank(provider.getUrl())) {
            throw new IllegalArgumentException("provider url is required");
        }
        if (isBlank(provider.getApiKey())) {
            throw new IllegalArgumentException("provider apiKey is required");
        }
        if (isBlank(provider.getModel())) {
            throw new IllegalArgumentException("provider model is required");
        }
        if (provider.getWeight() <= 0) {
            throw new IllegalArgumentException("provider weight must be greater than 0");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<AiProvider> getAvailableProviders() {
        List<AiProvider> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (AiProvider p : providers) {
            if (!p.isEnabled()) continue;
            if (p.isCircuitOpen()) {
                if (now - p.getLastFailTime() >= CIRCUIT_OPEN_DURATION_MS) {
                    result.add(p);
                }
                continue;
            }
            result.add(p);
        }
        return result;
    }

    private List<AiProvider> getHalfOpenProviders() {
        List<AiProvider> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (AiProvider p : providers) {
            if (p.isEnabled() && p.isCircuitOpen() && now - p.getLastFailTime() >= CIRCUIT_OPEN_DURATION_MS) {
                result.add(p);
            }
        }
        return result;
    }

    private AiProvider smoothWeightedSelect(List<AiProvider> candidates) {
        if (candidates.size() == 1) return candidates.get(0);

        // 计算有效权重：基础权重 * 延迟系数
        // 延迟越低，有效权重越高；没有延迟数据时用基础权重
        int totalEffectiveWeight = 0;
        AiProvider best = null;
        int bestCurrent = Integer.MIN_VALUE;

        for (AiProvider p : candidates) {
            int baseWeight = p.getWeight();
            double latency = p.getAvgLatencyMs();
            // 延迟系数：无数据=1.0, 100ms=1.0, 500ms=0.5, 1000ms=0.33
            double latencyFactor = latency > 0 ? 1000.0 / (1000.0 + latency) : 1.0;
            int effectiveWeight = Math.max(1, (int) Math.round(baseWeight * latencyFactor));

            int cw = currentWeight.getOrDefault(p.getName(), 0) + effectiveWeight;
            currentWeight.put(p.getName(), cw);
            totalEffectiveWeight += effectiveWeight;
            if (best == null || cw > bestCurrent) {
                best = p;
                bestCurrent = cw;
            }
        }

        if (best != null) {
            currentWeight.put(best.getName(), bestCurrent - totalEffectiveWeight);
        }
        return best;
    }
}
