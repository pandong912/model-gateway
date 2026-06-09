package com.example.modelgateway.service.config;

import com.example.modelgateway.api.enums.ModelCapability;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "model.gateway")
public class ModelGatewayProperties {
    private List<Provider> providers = new ArrayList<>();
    private List<Route> routes = new ArrayList<>();
    private Security security = new Security();
    private Quota quota = new Quota();

    public List<Provider> getProviders() {
        return providers;
    }

    public void setProviders(List<Provider> providers) {
        this.providers = providers;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Quota getQuota() {
        return quota;
    }

    public void setQuota(Quota quota) {
        this.quota = quota;
    }

    public static class Provider {
        private String id;
        private String type = "openai-compatible";
        private URI baseUrl;
        private String apiKey;
        private String organization;
        private Duration timeout = Duration.ofSeconds(60);

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public URI getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(URI baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getOrganization() {
            return organization;
        }

        public void setOrganization(String organization) {
            this.organization = organization;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    public static class Route {
        private String id;
        private String provider;
        private String model;
        private List<ModelCapability> capabilities = new ArrayList<>(List.of(ModelCapability.TEXT_GENERATION));
        private List<String> scenarios = new ArrayList<>();
        private int priority = 100;
        private boolean enabled = true;
        private List<String> fallbackRouteIds = new ArrayList<>();
        private Duration timeout = Duration.ofSeconds(60);
        private Map<String, Object> metadata = new HashMap<>();

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

        public List<ModelCapability> getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(List<ModelCapability> capabilities) {
            this.capabilities = capabilities;
        }

        public List<String> getScenarios() {
            return scenarios;
        }

        public void setScenarios(List<String> scenarios) {
            this.scenarios = scenarios;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getFallbackRouteIds() {
            return fallbackRouteIds;
        }

        public void setFallbackRouteIds(List<String> fallbackRouteIds) {
            this.fallbackRouteIds = fallbackRouteIds;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    public static class Security {
        private boolean enabled;
        private List<String> apiKeys = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getApiKeys() {
            return apiKeys;
        }

        public void setApiKeys(List<String> apiKeys) {
            this.apiKeys = apiKeys;
        }
    }

    public static class Quota {
        private boolean enabled;
        private int requestsPerMinute = 120;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
    }
}
