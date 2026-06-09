package com.example.modelgateway.service.config;

import com.example.modelgateway.core.provider.ProviderClient;
import com.example.modelgateway.core.provider.ProviderRegistry;
import com.example.modelgateway.core.quota.QuotaService;
import com.example.modelgateway.core.route.ModelRouter;
import com.example.modelgateway.core.route.RouteRepository;
import com.example.modelgateway.core.service.DefaultModelInvocationService;
import com.example.modelgateway.core.service.ModelInvocationService;
import com.example.modelgateway.core.service.PromptTemplateRepository;
import com.example.modelgateway.core.service.PromptTemplateService;
import com.example.modelgateway.core.usage.UsageRecorder;
import com.example.modelgateway.provider.gemini.GeminiProviderClient;
import com.example.modelgateway.provider.gemini.GeminiProviderConfig;
import com.example.modelgateway.provider.openai.OpenAiCompatibleProviderClient;
import com.example.modelgateway.provider.openai.OpenAiCompatibleProviderConfig;
import com.example.modelgateway.service.persistence.MybatisPlusPromptTemplateRepository;
import com.example.modelgateway.service.persistence.MybatisPlusRouteRepository;
import com.example.modelgateway.service.persistence.mapper.ModelRouteMapper;
import com.example.modelgateway.service.persistence.mapper.PromptTemplateAuditMapper;
import com.example.modelgateway.service.persistence.mapper.PromptTemplateMapper;
import com.example.modelgateway.service.usage.MeteredUsageRecorder;
import com.example.modelgateway.service.usage.SlidingWindowQuotaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GatewayConfiguration {
    @Bean
    RouteRepository routeRepository(ModelRouteMapper modelRouteMapper, ObjectMapper objectMapper) {
        return new MybatisPlusRouteRepository(modelRouteMapper, objectMapper);
    }

    @Bean
    ModelRouter modelRouter(RouteRepository routeRepository) {
        return new ModelRouter(routeRepository);
    }

    @Bean
    PromptTemplateRepository promptTemplateRepository(
            PromptTemplateMapper promptTemplateMapper,
            PromptTemplateAuditMapper promptTemplateAuditMapper,
            ObjectMapper objectMapper
    ) {
        return new MybatisPlusPromptTemplateRepository(promptTemplateMapper, promptTemplateAuditMapper, objectMapper);
    }

    @Bean
    PromptTemplateService promptTemplateService(PromptTemplateRepository promptTemplateRepository) {
        return new PromptTemplateService(promptTemplateRepository);
    }

    @Bean
    ProviderRegistry providerRegistry(
            ModelGatewayProperties properties,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper
    ) {
        List<ProviderClient> providers = properties.getProviders().stream()
                .map(provider -> createProvider(provider, webClientBuilder, objectMapper))
                .toList();
        return new ProviderRegistry(providers);
    }

    private ProviderClient createProvider(
            ModelGatewayProperties.Provider provider,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper
    ) {
        if ("gemini".equalsIgnoreCase(provider.getType())) {
            return new GeminiProviderClient(
                    new GeminiProviderConfig(provider.getId(), provider.getBaseUrl(), provider.getApiKey(), provider.getTimeout()),
                    webClientBuilder,
                    objectMapper);
        }
        return new OpenAiCompatibleProviderClient(
                new OpenAiCompatibleProviderConfig(
                        provider.getId(),
                        provider.getBaseUrl(),
                        provider.getApiKey(),
                        provider.getOrganization(),
                        provider.getTimeout()),
                webClientBuilder,
                objectMapper);
    }

    @Bean
    QuotaService quotaService(ModelGatewayProperties properties) {
        return new SlidingWindowQuotaService(properties.getQuota());
    }

    @Bean
    UsageRecorder usageRecorder(MeterRegistry meterRegistry) {
        return new MeteredUsageRecorder(meterRegistry);
    }

    @Bean
    ModelInvocationService modelInvocationService(
            ModelRouter modelRouter,
            RouteRepository routeRepository,
            ProviderRegistry providerRegistry,
            QuotaService quotaService,
            UsageRecorder usageRecorder,
            PromptTemplateService promptTemplateService
    ) {
        return new DefaultModelInvocationService(
                modelRouter,
                routeRepository,
                providerRegistry,
                quotaService,
                usageRecorder,
                promptTemplateService);
    }
}
