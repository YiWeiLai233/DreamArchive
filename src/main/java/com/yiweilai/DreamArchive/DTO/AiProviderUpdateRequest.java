package com.yiweilai.DreamArchive.DTO;

public class AiProviderUpdateRequest {

    private String url;
    private String apiKey;
    private String model;
    private Integer weight;
    private Boolean enabled;
    private Boolean visionEnabled;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Boolean getVisionEnabled() { return visionEnabled; }
    public void setVisionEnabled(Boolean visionEnabled) { this.visionEnabled = visionEnabled; }
}
