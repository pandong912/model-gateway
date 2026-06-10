package com.example.modelgateway.service.config;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "model.gateway")
public class ModelGatewayProperties {
    private List<Provider> providers = new ArrayList<>();
    private Security security = new Security();
    private Quota quota = new Quota();

    @Getter
    @Setter
    public static class Provider {
        private String id;
        private String type = "openai-compatible";
        private URI baseUrl;
        private String apiKey;
        private String organization;
        private String projectId;
        private String location;
        private String credentialsPath;
        private Duration timeout = Duration.ofSeconds(60);
    }

    @Getter
    @Setter
    public static class Security {
        private boolean enabled;
        private List<String> apiKeys = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Quota {
        private boolean enabled;
        private int requestsPerMinute = 120;
    }
}
