package com.example.modelgateway.service.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("model_routes")
public class ModelRouteEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    private String provider;
    private String model;
    private String capabilities;
    private String scenarios;
    private Integer priority;
    private Boolean enabled;
    private String fallbackRouteIds;
    private Long timeoutSeconds;
    private String metadata;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(String capabilities) {
        this.capabilities = capabilities;
    }

    public String getScenarios() {
        return scenarios;
    }

    public void setScenarios(String scenarios) {
        this.scenarios = scenarios;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getFallbackRouteIds() {
        return fallbackRouteIds;
    }

    public void setFallbackRouteIds(String fallbackRouteIds) {
        this.fallbackRouteIds = fallbackRouteIds;
    }

    public Long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
