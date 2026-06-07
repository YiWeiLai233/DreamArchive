package com.yiweilai.DreamArchive.service;

import com.yiweilai.DreamArchive.DTO.AiProvider;
import com.yiweilai.DreamArchive.DTO.AiProviderUpdateRequest;
import com.yiweilai.DreamArchive.mapper.AiProviderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiProviderPool {

    private static final Logger log = LoggerFactory.getLogger(AiProviderPool.class);

    private static final int MAX_FAIL_COUNT = 3;
    private static final long CIRCUIT_OPEN_DURATION_MS = 60_000;

    private List<AiProvider> providers = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> currentWeight = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private AiProviderMapper aiProviderMapper;

    public List<AiProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }

    public void setProviders(List<AiProvider> providers) {
        this.providers = providers != null ? new CopyOnWriteArrayList<>(providers) : new CopyOnWriteArrayList<>();
        rebuildCurrentWeights();
    }

    public String getVisionProvider() {
        for (AiProvider provider : providers) {
            if (provider.isEnabled() && provider.isVisionEnabled()) {
                return provider.getName();
            }
        }
        return null;
    }

    @PostConstruct
    public void init() {
        loadProvidersFromDatabase();
        if (providers.isEmpty()) {
            log.warn("AI pool has no providers. Add providers in ai_provider table or Admin AI pool page.");
        } else {
            for (AiProvider p : providers) {
                currentWeight.putIfAbsent(p.getName(), 0);
                log.info("AI provider loaded: {} (weight={}, enabled={})", p.getName(), p.getWeight(), p.isEnabled());
            }
        }
    }

    public synchronized void refreshFromDatabase() {
        loadProvidersFromDatabase();
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

    public synchronized AiProvider acquire(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return acquire();
        }
        for (AiProvider p : providers) {
            if (p.getName().equals(providerName)) {
                if (!p.isEnabled()) {
                    throw new RuntimeException("AI provider [" + providerName + "] is disabled");
                }
                if (p.isCircuitOpen()) {
                    long now = System.currentTimeMillis();
                    if (now - p.getLastFailTime() >= CIRCUIT_OPEN_DURATION_MS) {
                        return p;
                    }
                    throw new RuntimeException("AI provider [" + providerName + "] circuit is open");
                }
                return p;
            }
        }
        throw new RuntimeException("AI provider [" + providerName + "] not found");
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

    @Transactional
    public synchronized void addProvider(AiProvider provider) {
        validateProvider(provider);
        normalizeProvider(provider);
        if (findProvider(provider.getName()) != null) {
            throw new IllegalArgumentException("provider [" + provider.getName() + "] already exists");
        }
        if (aiProviderMapper != null) {
            if (aiProviderMapper.selectByName(provider.getName()) != null) {
                throw new IllegalArgumentException("provider [" + provider.getName() + "] already exists");
            }
            if (provider.isVisionEnabled()) {
                aiProviderMapper.clearVisionEnabledExcept(provider.getName());
            }
            aiProviderMapper.insert(provider);
        }
        if (provider.isVisionEnabled()) {
            clearMemoryVisionEnabledExcept(provider.getName());
        }
        currentWeight.put(provider.getName(), 0);
        providers.add(provider);
    }

    @Transactional
    public synchronized boolean removeProvider(String name) {
        if (isBlank(name)) {
            return false;
        }
        if (aiProviderMapper != null && aiProviderMapper.deleteByName(name) == 0) {
            return false;
        }
        boolean removed = providers.removeIf(p -> p.getName().equals(name));
        if (removed) currentWeight.remove(name);
        return removed;
    }

    @Transactional
    public AiProvider updateProvider(String name, Integer weight, Boolean enabled) {
        AiProviderUpdateRequest request = new AiProviderUpdateRequest();
        request.setWeight(weight);
        request.setEnabled(enabled);
        return updateProvider(name, request);
    }

    @Transactional
    public AiProvider updateProvider(String name, AiProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider is required");
        }
        AiProviderUpdateRequest request = new AiProviderUpdateRequest();
        request.setUrl(provider.getUrl());
        request.setApiKey(provider.getApiKey());
        request.setModel(provider.getModel());
        request.setWeight(provider.getWeight());
        request.setEnabled(provider.isEnabled());
        request.setVisionEnabled(provider.isVisionEnabled());
        return updateProvider(name, request);
    }

    @Transactional
    public synchronized AiProvider updateProvider(String name, AiProviderUpdateRequest request) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("provider name is required");
        }
        AiProvider existing = findProvider(name);
        if (existing == null) {
            return null;
        }
        AiProvider updated = mergeProvider(existing, request);
        validateProvider(updated);

        if (aiProviderMapper != null) {
            if (updated.isVisionEnabled()) {
                aiProviderMapper.clearVisionEnabledExcept(updated.getName());
            }
            aiProviderMapper.update(updated);
        }

        if (updated.isVisionEnabled()) {
            clearMemoryVisionEnabledExcept(updated.getName());
        }
        applyProvider(existing, updated);
        currentWeight.putIfAbsent(existing.getName(), 0);
        return existing;
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

    private void loadProvidersFromDatabase() {
        if (aiProviderMapper == null) {
            rebuildCurrentWeights();
            return;
        }
        try {
            setProviders(aiProviderMapper.selectAll());
        } catch (RuntimeException e) {
            log.error("Failed to load AI providers from database", e);
            setProviders(List.of());
        }
    }

    private void rebuildCurrentWeights() {
        currentWeight.clear();
        for (AiProvider provider : providers) {
            if (!isBlank(provider.getName())) {
                currentWeight.put(provider.getName(), 0);
            }
        }
    }

    private AiProvider findProvider(String name) {
        if (isBlank(name)) {
            return null;
        }
        for (AiProvider provider : providers) {
            if (provider.getName().equals(name)) {
                return provider;
            }
        }
        return null;
    }

    private AiProvider mergeProvider(AiProvider existing, AiProviderUpdateRequest request) {
        AiProvider updated = new AiProvider(
                existing.getName(),
                existing.getUrl(),
                existing.getApiKey(),
                existing.getModel(),
                existing.getWeight(),
                existing.isEnabled()
        );
        updated.setVisionEnabled(existing.isVisionEnabled());

        if (request == null) {
            return updated;
        }
        if (request.getUrl() != null) {
            updated.setUrl(request.getUrl());
        }
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            updated.setApiKey(request.getApiKey());
        }
        if (request.getModel() != null) {
            updated.setModel(request.getModel());
        }
        if (request.getWeight() != null) {
            updated.setWeight(request.getWeight());
        }
        if (request.getEnabled() != null) {
            updated.setEnabled(request.getEnabled());
        }
        if (request.getVisionEnabled() != null) {
            updated.setVisionEnabled(request.getVisionEnabled());
        }
        normalizeProvider(updated);
        return updated;
    }

    private void normalizeProvider(AiProvider provider) {
        provider.setName(trim(provider.getName()));
        provider.setUrl(trim(provider.getUrl()));
        provider.setApiKey(trim(provider.getApiKey()));
        provider.setModel(trim(provider.getModel()));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void clearMemoryVisionEnabledExcept(String name) {
        for (AiProvider provider : providers) {
            if (!provider.getName().equals(name)) {
                provider.setVisionEnabled(false);
            }
        }
    }

    private void applyProvider(AiProvider target, AiProvider source) {
        target.setUrl(source.getUrl());
        target.setApiKey(source.getApiKey());
        target.setModel(source.getModel());
        target.setWeight(source.getWeight());
        target.setEnabled(source.isEnabled());
        target.setVisionEnabled(source.isVisionEnabled());
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
