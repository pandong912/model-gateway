package com.example.modelgateway.provider.gemini;

import com.example.modelgateway.api.enums.FinishReason;
import com.example.modelgateway.api.enums.MessageRole;
import com.example.modelgateway.api.enums.ModelCapability;
import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.api.model.ChatMessage;
import com.example.modelgateway.api.model.ChatStreamEvent;
import com.example.modelgateway.api.model.GeneratedMedia;
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
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GeminiProviderClient implements ProviderClient {
    protected final GeminiProviderConfig config;
    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;

    public GeminiProviderClient(GeminiProviderConfig config, WebClient.Builder builder, ObjectMapper objectMapper) {
        this(config, builder.clone()
                .baseUrl(config.baseUrl().toString())
                .defaultHeader("x-goog-api-key", config.apiKey() == null ? "" : config.apiKey())
                .build(), objectMapper);
    }

    protected GeminiProviderClient(GeminiProviderConfig config, WebClient webClient, ObjectMapper objectMapper) {
        this.config = config;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerId() {
        return config.id();
    }

    @Override
    public Mono<ChatCompletionResponse> chat(ChatCompletionRequest request, ModelRoute route, InvocationContext context) {
        Instant start = Instant.now();
        Map<String, Object> body = toGeminiRequest(request, route);
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/models/{model}:generateContent").build(route.model()))
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
        Map<String, Object> body = toGeminiRequest(request, route);
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/models/{model}:streamGenerateContent")
                        .queryParam("alt", "sse")
                        .build(route.model()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(chunk -> parseStreamChunk(chunk, responseId, route, context))
                .concatWith(Mono.just(ChatStreamEvent.done(responseId, TokenUsage.empty(), route.provider(), route.model(), route.id(), context.traceId())));
    }

    protected Map<String, Object> toGeminiRequest(ChatCompletionRequest request, ModelRoute route) {
        Map<String, Object> body = new LinkedHashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        List<Map<String, Object>> systemParts = new ArrayList<>();

        for (ChatMessage message : request.messages()) {
            if (message.role() == MessageRole.SYSTEM) {
                if (message.content() != null && !message.content().isBlank()) {
                    systemParts.add(Map.of("text", message.content()));
                }
            } else {
                contents.add(toGeminiContent(message));
            }
        }

        if (!systemParts.isEmpty()) {
            body.put("systemInstruction", Map.of("parts", systemParts));
        }
        body.put("contents", contents);
        body.put("generationConfig", generationConfig(request, route));

        Object safetySettings = request.safeParameters().get("safetySettings");
        if (safetySettings != null) {
            body.put("safetySettings", safetySettings);
        }
        Object tools = request.safeParameters().get("tools");
        if (tools != null) {
            body.put("tools", tools);
        }
        return body;
    }

    protected Map<String, Object> toGeminiContent(ChatMessage message) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", message.role() == MessageRole.ASSISTANT ? "model" : "user");
        List<Map<String, Object>> parts = new ArrayList<>();
        if (message.content() != null && !message.content().isBlank()) {
            parts.add(Map.of("text", message.content()));
        }
        if (message.media() != null) {
            message.media().stream()
                    .map(this::toGeminiMediaPart)
                    .forEach(parts::add);
        }
        content.put("parts", parts);
        return content;
    }

    protected Map<String, Object> toGeminiMediaPart(MediaContent media) {
        String mimeType = resolveMimeType(media);
        if (media.base64() != null && !media.base64().isBlank()) {
            return Map.of("inlineData", Map.of(
                    "mimeType", mimeType,
                    "data", media.base64()));
        }
        String fileUri = media.url();
        if (media.metadata() != null && media.metadata().get("geminiFileUri") != null) {
            fileUri = String.valueOf(media.metadata().get("geminiFileUri"));
        }
        if (fileUri != null && !fileUri.isBlank()) {
            return Map.of("fileData", Map.of(
                    "mimeType", mimeType,
                    "fileUri", fileUri));
        }
        return Map.of("text", "");
    }

    protected String resolveMimeType(MediaContent media) {
        if (media.mimeType() != null && !media.mimeType().isBlank()) {
            return media.mimeType();
        }
        if ("video".equalsIgnoreCase(media.type())) {
            return "video/mp4";
        }
        if ("audio".equalsIgnoreCase(media.type())) {
            return "audio/mpeg";
        }
        return "image/jpeg";
    }

    protected Map<String, Object> generationConfig(ChatCompletionRequest request, ModelRoute route) {
        Map<String, Object> configMap = new LinkedHashMap<>();
        Map<String, Object> parameters = request.safeParameters();
        copyIfPresent(parameters, configMap, "temperature", "temperature");
        copyIfPresent(parameters, configMap, "topP", "topP");
        copyIfPresent(parameters, configMap, "top_p", "topP");
        copyIfPresent(parameters, configMap, "topK", "topK");
        copyIfPresent(parameters, configMap, "top_k", "topK");
        copyIfPresent(parameters, configMap, "maxOutputTokens", "maxOutputTokens");
        copyIfPresent(parameters, configMap, "max_tokens", "maxOutputTokens");
        copyIfPresent(parameters, configMap, "candidateCount", "candidateCount");
        copyIfPresent(parameters, configMap, "stopSequences", "stopSequences");
        copyIfPresent(parameters, configMap, "thinkingConfig", "thinkingConfig");
        copyIfPresent(parameters, configMap, "responseSchema", "responseSchema");
        copyIfPresent(parameters, configMap, "responseModalities", "responseModalities");

        Object responseMimeType = parameters.get("responseMimeType");
        if (responseMimeType != null) {
            configMap.put("responseMimeType", responseMimeType);
        } else if (route.capabilities().contains(ModelCapability.JSON_MODE) || isJsonMode(parameters)) {
            configMap.put("responseMimeType", "application/json");
        }
        if (!configMap.containsKey("responseModalities")
                && (route.capabilities().contains(ModelCapability.IMAGE_GENERATION)
                || route.capabilities().contains(ModelCapability.IMAGE_EDITING))) {
            configMap.put("responseModalities", List.of("TEXT", "IMAGE"));
        }
        return configMap;
    }

    protected boolean isJsonMode(Map<String, Object> parameters) {
        Object responseFormat = parameters.get("response_format");
        return responseFormat instanceof Map<?, ?> map && "json_object".equals(map.get("type"));
    }

    protected void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String sourceKey, String targetKey) {
        Object value = source.get(sourceKey);
        if (value != null) {
            target.put(targetKey, value);
        }
    }

    protected ChatCompletionResponse toGatewayResponse(JsonNode response, ModelRoute route, InvocationContext context, long latencyMs) {
        JsonNode candidate = first(response.path("candidates"));
        JsonNode responseContent = candidate.path("content");
        String content = extractText(responseContent);
        List<GeneratedMedia> mediaOutputs = extractMedia(responseContent);
        return new ChatCompletionResponse(
                UUID.randomUUID().toString(),
                content,
                List.of(),
                mediaOutputs,
                finishReason(candidate.path("finishReason").asText()),
                usage(response.path("usageMetadata")),
                route.provider(),
                route.model(),
                route.id(),
                context.traceId(),
                latencyMs,
                Map.of("adapter", "gemini-native"));
    }

    protected Flux<ChatStreamEvent> parseStreamChunk(String chunk, String responseId, ModelRoute route, InvocationContext context) {
        return Flux.fromArray(chunk.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring("data:".length()).trim())
                .filter(data -> !data.isBlank())
                .flatMap(data -> parseStreamData(data, responseId, route, context));
    }

    protected Flux<ChatStreamEvent> parseStreamData(String data, String responseId, ModelRoute route, InvocationContext context) {
        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode content = first(node.path("candidates")).path("content");
            String delta = extractText(content);
            List<GeneratedMedia> mediaOutputs = extractMedia(content);
            List<ChatStreamEvent> events = new ArrayList<>();
            if (!delta.isBlank()) {
                events.add(ChatStreamEvent.delta(responseId, delta, route.provider(), route.model(), route.id(), context.traceId()));
            }
            if (!mediaOutputs.isEmpty()) {
                events.add(ChatStreamEvent.media(responseId, mediaOutputs, route.provider(), route.model(), route.id(), context.traceId()));
            }
            return Flux.fromIterable(events);
        } catch (Exception ex) {
            return Flux.error(ex);
        }
    }

    protected JsonNode first(JsonNode array) {
        return array.isArray() && !array.isEmpty() ? array.get(0) : objectMapper.createObjectNode();
    }

    protected String extractText(JsonNode content) {
        JsonNode parts = content.path("parts");
        if (!parts.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode part : parts) {
            String text = part.path("text").asText("");
            if (!text.isBlank()) {
                builder.append(text);
            }
        }
        return builder.toString();
    }

    protected List<GeneratedMedia> extractMedia(JsonNode content) {
        JsonNode parts = content.path("parts");
        if (!parts.isArray()) {
            return List.of();
        }
        List<GeneratedMedia> mediaOutputs = new ArrayList<>();
        for (JsonNode part : parts) {
            JsonNode inlineData = part.has("inlineData") ? part.path("inlineData") : part.path("inline_data");
            if (inlineData.isMissingNode() || inlineData.isNull()) {
                continue;
            }
            String mimeType = inlineData.path("mimeType").asText(inlineData.path("mime_type").asText("application/octet-stream"));
            String base64 = inlineData.path("data").asText("");
            if (!base64.isBlank()) {
                String type = mimeType.startsWith("image/") ? "image" : "media";
                mediaOutputs.add(new GeneratedMedia(
                        type,
                        mimeType,
                        base64,
                        null,
                        Map.of("source", "gemini-inline-data")));
            }
        }
        return mediaOutputs;
    }

    protected FinishReason finishReason(String value) {
        return switch (value) {
            case "STOP" -> FinishReason.STOP;
            case "MAX_TOKENS" -> FinishReason.LENGTH;
            case "SAFETY", "RECITATION", "SPII", "PROHIBITED_CONTENT" -> FinishReason.CONTENT_FILTER;
            default -> FinishReason.UNKNOWN;
        };
    }

    protected TokenUsage usage(JsonNode usage) {
        long promptTokens = usage.path("promptTokenCount").asLong(0);
        long completionTokens = usage.path("candidatesTokenCount").asLong(0);
        long totalTokens = usage.path("totalTokenCount").asLong(promptTokens + completionTokens);
        return new TokenUsage(promptTokens, completionTokens, totalTokens, BigDecimal.ZERO);
    }
}
