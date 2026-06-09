package com.example.modelgateway.provider.openai;

import com.example.modelgateway.api.enums.FinishReason;
import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.api.model.ChatMessage;
import com.example.modelgateway.api.model.ChatStreamEvent;
import com.example.modelgateway.api.model.MediaContent;
import com.example.modelgateway.api.model.TokenUsage;
import com.example.modelgateway.core.model.InvocationContext;
import com.example.modelgateway.core.model.ModelRoute;
import com.example.modelgateway.core.provider.ProviderClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class OpenAiCompatibleProviderClient implements ProviderClient {
    private final OpenAiCompatibleProviderConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleProviderClient(OpenAiCompatibleProviderConfig config, WebClient.Builder builder, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        WebClient.Builder configuredBuilder = builder.clone()
                .baseUrl(config.baseUrl().toString())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.apiKey());
        if (config.organization() != null && !config.organization().isBlank()) {
            configuredBuilder.defaultHeader("OpenAI-Organization", config.organization());
        }
        this.webClient = configuredBuilder.build();
    }

    @Override
    public String providerId() {
        return config.id();
    }

    @Override
    public Mono<ChatCompletionResponse> chat(ChatCompletionRequest request, ModelRoute route, InvocationContext context) {
        Instant start = Instant.now();
        Map<String, Object> body = toOpenAiRequest(request, route, false);
        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> toGatewayResponse(response, route, context, Duration.between(start, Instant.now()).toMillis()));
    }

    @Override
    public Flux<ChatStreamEvent> stream(ChatCompletionRequest request, ModelRoute route, InvocationContext context) {
        String responseId = UUID.randomUUID().toString();
        Map<String, Object> body = toOpenAiRequest(request, route, true);
        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(chunk -> parseStreamChunk(chunk, responseId, route, context))
                .concatWith(Mono.just(ChatStreamEvent.done(responseId, TokenUsage.empty(), route.provider(), route.model(), route.id(), context.traceId())));
    }

    private Map<String, Object> toOpenAiRequest(ChatCompletionRequest request, ModelRoute route, boolean stream) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", route.model());
        body.put("messages", request.messages().stream().map(this::toOpenAiMessage).toList());
        body.put("stream", stream);
        body.putAll(request.safeParameters());
        return body;
    }

    private Map<String, Object> toOpenAiMessage(ChatMessage message) {
        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("role", message.role().name().toLowerCase());
        if (message.media() == null || message.media().isEmpty()) {
            mapped.put("content", message.content() == null ? "" : message.content());
        } else {
            List<Map<String, Object>> content = new ArrayList<>();
            if (message.content() != null && !message.content().isBlank()) {
                content.add(Map.of("type", "text", "text", message.content()));
            }
            for (MediaContent media : message.media()) {
                content.add(toMediaPart(media));
            }
            mapped.put("content", content);
        }
        if (message.name() != null && !message.name().isBlank()) {
            mapped.put("name", message.name());
        }
        return mapped;
    }

    private Map<String, Object> toMediaPart(MediaContent media) {
        if ("image".equalsIgnoreCase(media.type())) {
            String imageUrl = media.url();
            if ((imageUrl == null || imageUrl.isBlank()) && media.base64() != null) {
                imageUrl = "data:" + media.mimeType() + ";base64," + media.base64();
            }
            return Map.of("type", "image_url", "image_url", Map.of("url", imageUrl == null ? "" : imageUrl));
        }
        return Map.of("type", "text", "text", media.url() == null ? "" : media.url());
    }

    private ChatCompletionResponse toGatewayResponse(JsonNode response, ModelRoute route, InvocationContext context, long latencyMs) {
        JsonNode choice = response.path("choices").isArray() && response.path("choices").size() > 0
                ? response.path("choices").get(0)
                : objectMapper.createObjectNode();
        String content = choice.path("message").path("content").asText("");
        return new ChatCompletionResponse(
                response.path("id").asText(UUID.randomUUID().toString()),
                content,
                List.of(),
                List.of(),
                finishReason(choice.path("finish_reason").asText()),
                usage(response.path("usage")),
                route.provider(),
                route.model(),
                route.id(),
                context.traceId(),
                latencyMs,
                Map.of("providerResponseModel", response.path("model").asText(route.model())));
    }

    private Flux<ChatStreamEvent> parseStreamChunk(String chunk, String responseId, ModelRoute route, InvocationContext context) {
        return Flux.fromArray(chunk.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring("data:".length()).trim())
                .filter(data -> !data.isBlank())
                .filter(data -> !"[DONE]".equals(data))
                .flatMap(data -> parseStreamData(data, responseId, route, context));
    }

    private Mono<ChatStreamEvent> parseStreamData(String data, String responseId, ModelRoute route, InvocationContext context) {
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode choice = node.path("choices").isArray() && node.path("choices").size() > 0
                    ? node.path("choices").get(0)
                    : objectMapper.createObjectNode();
            String delta = choice.path("delta").path("content").asText("");
            if (delta.isBlank()) {
                return Mono.empty();
            }
            String id = node.path("id").asText(responseId);
            return Mono.just(ChatStreamEvent.delta(id, delta, route.provider(), route.model(), route.id(), context.traceId()));
        } catch (Exception ex) {
            return Mono.error(ex);
        }
    }

    private FinishReason finishReason(String value) {
        return switch (value) {
            case "stop" -> FinishReason.STOP;
            case "length" -> FinishReason.LENGTH;
            case "tool_calls" -> FinishReason.TOOL_CALLS;
            case "content_filter" -> FinishReason.CONTENT_FILTER;
            default -> FinishReason.UNKNOWN;
        };
    }

    private TokenUsage usage(JsonNode usage) {
        long promptTokens = usage.path("prompt_tokens").asLong(0);
        long completionTokens = usage.path("completion_tokens").asLong(0);
        long totalTokens = usage.path("total_tokens").asLong(promptTokens + completionTokens);
        return new TokenUsage(promptTokens, completionTokens, totalTokens, BigDecimal.ZERO);
    }
}
