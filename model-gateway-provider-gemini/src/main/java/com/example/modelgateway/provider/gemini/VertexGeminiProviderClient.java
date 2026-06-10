package com.example.modelgateway.provider.gemini;

import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.api.model.ChatStreamEvent;
import com.example.modelgateway.api.model.TokenUsage;
import com.example.modelgateway.core.model.InvocationContext;
import com.example.modelgateway.core.model.ModelRoute;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class VertexGeminiProviderClient extends GeminiProviderClient {
    private final VertexGeminiProviderConfig vertexConfig;
    private final VertexGeminiAccessTokenProvider accessTokenProvider;

    public VertexGeminiProviderClient(
            VertexGeminiProviderConfig config,
            WebClient.Builder builder,
            ObjectMapper objectMapper
    ) {
        super(new GeminiProviderConfig(config.id(), vertexBaseUrl(config), null, config.timeout()),
                builder.clone().baseUrl(vertexBaseUrl(config).toString()).build(),
                objectMapper);
        this.vertexConfig = config;
        this.accessTokenProvider = new VertexGeminiAccessTokenProvider(config.credentialsPath());
    }

    @Override
    public Mono<ChatCompletionResponse> chat(ChatCompletionRequest request, ModelRoute route, InvocationContext context) {
        Instant start = Instant.now();
        Map<String, Object> body = toGeminiRequest(request, route);
        return accessToken()
                .flatMap(accessToken -> webClient.post()
                        .uri(uriBuilder -> uriBuilder.path("/models/{model}:generateContent").build(route.model()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(JsonNode.class))
                .map(response -> toGatewayResponse(response, route, context, Duration.between(start, Instant.now()).toMillis()));
    }

    @Override
    public Flux<ChatStreamEvent> stream(ChatCompletionRequest request, ModelRoute route, InvocationContext context) {
        String responseId = UUID.randomUUID().toString();
        Map<String, Object> body = toGeminiRequest(request, route);
        return accessToken()
                .flatMapMany(accessToken -> webClient.post()
                        .uri(uriBuilder -> uriBuilder.path("/models/{model}:streamGenerateContent")
                                .queryParam("alt", "sse")
                                .build(route.model()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToFlux(String.class))
                .flatMap(chunk -> parseStreamChunk(chunk, responseId, route, context))
                .concatWith(Mono.just(ChatStreamEvent.done(responseId, TokenUsage.empty(), route.provider(), route.model(), route.id(), context.traceId())));
    }

    @Override
    public String providerId() {
        return vertexConfig.id();
    }

    private Mono<String> accessToken() {
        return Mono.fromCallable(accessTokenProvider::accessToken);
    }

    private static URI vertexBaseUrl(VertexGeminiProviderConfig config) {
        if (config.baseUrl() != null) {
            return config.baseUrl();
        }
        String projectId = config.projectId() == null || config.projectId().isBlank() ? "missing-project-id" : config.projectId();
        String location = config.location() == null || config.location().isBlank() ? "us-central1" : config.location();
        String host = "global".equalsIgnoreCase(location)
                ? "https://aiplatform.googleapis.com"
                : "https://%s-aiplatform.googleapis.com".formatted(location);
        return URI.create("%s/v1/projects/%s/locations/%s/publishers/google"
                .formatted(host, projectId, location));
    }
}
