package com.example.modelgateway.client;

import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.api.model.ChatStreamEvent;
import com.example.modelgateway.core.model.PromptTemplate;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class WebClientModelGatewayClient implements ModelGatewayClient {
    private final WebClient webClient;

    public WebClientModelGatewayClient(WebClient.Builder builder, ModelGatewayClientProperties properties) {
        WebClient.Builder configuredBuilder = builder.clone()
                .baseUrl(properties.getBaseUrl().toString());
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            configuredBuilder.defaultHeader("X-API-Key", properties.getApiKey());
        }
        this.webClient = configuredBuilder.build();
    }

    @Override
    public Mono<ChatCompletionResponse> chat(ChatCompletionRequest request) {
        return webClient.post()
                .uri("/api/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class);
    }

    @Override
    public Flux<ChatStreamEvent> stream(ChatCompletionRequest request) {
        return webClient.post()
                .uri("/api/v1/chat/completions/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(ChatStreamEvent.class);
    }

    @Override
    public Mono<List<PromptTemplate>> prompts() {
        return webClient.get()
                .uri("/admin/v1/prompts")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(PromptTemplate.class)
                .collectList();
    }

    @Override
    public Mono<PromptTemplate> savePrompt(PromptTemplate promptTemplate) {
        return webClient.post()
                .uri("/admin/v1/prompts")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(promptTemplate)
                .retrieve()
                .bodyToMono(PromptTemplate.class);
    }
}
