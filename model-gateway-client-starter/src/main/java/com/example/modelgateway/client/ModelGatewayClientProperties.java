package com.example.modelgateway.client;

import java.net.URI;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "model.gateway.client")
public class ModelGatewayClientProperties {
    private URI baseUrl = URI.create("http://localhost:8080");
    private String apiKey;
}
