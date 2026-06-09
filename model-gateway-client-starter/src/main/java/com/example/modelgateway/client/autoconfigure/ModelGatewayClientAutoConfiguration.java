package com.example.modelgateway.client.autoconfigure;

import com.example.modelgateway.client.ModelGatewayClient;
import com.example.modelgateway.client.ModelGatewayClientProperties;
import com.example.modelgateway.client.WebClientModelGatewayClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
@EnableConfigurationProperties(ModelGatewayClientProperties.class)
public class ModelGatewayClientAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    ModelGatewayClient modelGatewayClient(WebClient.Builder builder, ModelGatewayClientProperties properties) {
        return new WebClientModelGatewayClient(builder, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    WebClient.Builder modelGatewayWebClientBuilder() {
        return WebClient.builder();
    }
}
