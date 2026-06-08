package com.yiweilai.DreamArchive.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiProvider {

    private String name;
    private String url;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String apiKey;
    private String model;
    private int weight = 10;
    private boolean enabled = true;
    private boolean visionEnabled;

    // 运行时状态（不持久化）
    private int failCount;
    private long lastFailTime;
    private boolean circuitOpen;
    private volatile double avgLatencyMs; // 指数移动平均延迟

    public AiProvider() {}

    public AiProvider(String name, String url, String apiKey, String model, int weight, boolean enabled) {
        this.name = name;
        this.url = url;
        this.apiKey = apiKey;
        this.model = model;
        this.weight = weight;
        this.enabled = enabled;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isVisionEnabled() { return visionEnabled; }
    public void setVisionEnabled(boolean visionEnabled) { this.visionEnabled = visionEnabled; }

    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }

    public long getLastFailTime() { return lastFailTime; }
    public void setLastFailTime(long lastFailTime) { this.lastFailTime = lastFailTime; }

    public boolean isCircuitOpen() { return circuitOpen; }
    public void setCircuitOpen(boolean circuitOpen) { this.circuitOpen = circuitOpen; }

    public double getAvgLatencyMs() { return avgLatencyMs; }
    public void setAvgLatencyMs(double avgLatencyMs) { this.avgLatencyMs = avgLatencyMs; }
}
