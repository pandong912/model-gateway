package com.example.kling.inference.service.backend;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.kling.inference.contract.model.ImageGenerationPayload;
import com.example.kling.inference.contract.model.InputAsset;
import com.example.kling.inference.contract.model.KlingGenerationPayload;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.VideoGenerationPayload;
import com.example.kling.inference.core.InferenceBackendClient;

@Service
@Profile("real-kling-backend")
public class KlingInternalInferenceBackendClient implements InferenceBackendClient {

    private final WebClient webClient;
    private final String textToVideoSubmitPath;
    private final String imageToVideoSubmitPath;
    private final String imageGenerationSubmitPath;
    private final String imageEditingSubmitPath;
    private final String cancelPathTemplate;
    private final String accessKey;
    private final String secretKey;

    public KlingInternalInferenceBackendClient(
        WebClient.Builder webClientBuilder,
        @Value("${kling.inference.backend.base-url}") String baseUrl,
        @Value("${kling.inference.backend.text-to-video-submit-path:/v1/videos/text2video}") String textToVideoSubmitPath,
        @Value("${kling.inference.backend.image-to-video-submit-path:/v1/videos/image2video}") String imageToVideoSubmitPath,
        @Value("${kling.inference.backend.image-generation-submit-path:/v1/images/generations}") String imageGenerationSubmitPath,
        @Value("${kling.inference.backend.image-editing-submit-path:/v1/images/edits}") String imageEditingSubmitPath,
        @Value("${kling.inference.backend.cancel-path-template:/internal/inference/tasks/{backendTaskId}/cancel}") String cancelPathTemplate,
        @Value("${kling.inference.backend.access-key:}") String accessKey,
        @Value("${kling.inference.backend.secret-key:}") String secretKey
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.textToVideoSubmitPath = textToVideoSubmitPath;
        this.imageToVideoSubmitPath = imageToVideoSubmitPath;
        this.imageGenerationSubmitPath = imageGenerationSubmitPath;
        this.imageEditingSubmitPath = imageEditingSubmitPath;
        this.cancelPathTemplate = cancelPathTemplate;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Override
    public Mono<BackendSubmission> submit(KlingGenerationRequest<? extends KlingGenerationPayload> request) {
        BackendSubmitRequest submitRequest = buildSubmitRequest(request);
        return webClient.post()
            .uri(submitRequest.path())
            .headers(headers -> headers.setBearerAuth(sign(accessKey, secretKey)))
            .bodyValue(submitRequest.body())
            .retrieve()
            .bodyToMono(KlingBackendSubmissionResponse.class)
            .map(response -> new BackendSubmission(
                response.backendTaskId(),
                response.backendProvider(),
                response.traceId()
            ));
    }

    private BackendSubmitRequest buildSubmitRequest(KlingGenerationRequest<? extends KlingGenerationPayload> request) {
        return switch (request.generationType()) {
            case TEXT_TO_VIDEO -> new BackendSubmitRequest(
                textToVideoSubmitPath,
                buildTextToVideoRequest(request, requirePayload(request, VideoGenerationPayload.class))
            );
            case IMAGE_TO_VIDEO -> new BackendSubmitRequest(
                imageToVideoSubmitPath,
                buildImageToVideoRequest(request, requirePayload(request, VideoGenerationPayload.class))
            );
            case IMAGE_GENERATION -> new BackendSubmitRequest(
                imageGenerationSubmitPath,
                buildImageGenerationRequest(request, requirePayload(request, ImageGenerationPayload.class))
            );
            case IMAGE_EDITING -> new BackendSubmitRequest(
                imageEditingSubmitPath,
                buildImageEditingRequest(request, requirePayload(request, ImageGenerationPayload.class))
            );
            default -> throw new IllegalArgumentException("Unsupported open platform generation type: " + request.generationType());
        };
    }

    private Map<String, Object> buildTextToVideoRequest(
        KlingGenerationRequest<? extends KlingGenerationPayload> request,
        VideoGenerationPayload payload
    ) {
        Map<String, Object> body = commonBody(request);
        body.put("prompt", payload.prompt());
        body.put("negative_prompt", payload.negativePrompt());
        body.put("duration", String.valueOf(payload.durationSeconds()));
        body.put("aspect_ratio", payload.aspectRatio());
        body.put("mode", payload.mode());
        body.put("seed", payload.seed());
        body.put("parameters", nullToEmpty(payload.parameters()));
        return body;
    }

    private Map<String, Object> buildImageToVideoRequest(
        KlingGenerationRequest<? extends KlingGenerationPayload> request,
        VideoGenerationPayload payload
    ) {
        Map<String, Object> body = buildTextToVideoRequest(request, payload);
        body.put("image_assets", assets(payload.inputAssets()));
        return body;
    }

    private Map<String, Object> buildImageGenerationRequest(
        KlingGenerationRequest<? extends KlingGenerationPayload> request,
        ImageGenerationPayload payload
    ) {
        Map<String, Object> body = commonBody(request);
        body.put("prompt", payload.prompt());
        body.put("negative_prompt", payload.negativePrompt());
        body.put("resolution", payload.resolution());
        body.put("seed", payload.seed());
        body.put("parameters", nullToEmpty(payload.parameters()));
        return body;
    }

    private Map<String, Object> buildImageEditingRequest(
        KlingGenerationRequest<? extends KlingGenerationPayload> request,
        ImageGenerationPayload payload
    ) {
        Map<String, Object> body = buildImageGenerationRequest(request, payload);
        body.put("image_assets", assets(payload.inputAssets()));
        return body;
    }

    private Map<String, Object> commonBody(KlingGenerationRequest<? extends KlingGenerationPayload> request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("request_id", request.requestId());
        body.put("model_name", request.model());
        body.put("scenario", request.scenario());
        body.put("priority", request.priority());
        body.put("metadata", nullToEmpty(request.metadata()));
        return body;
    }

    private List<Map<String, Object>> assets(List<InputAsset> inputAssets) {
        if (inputAssets == null) {
            return List.of();
        }
        return inputAssets.stream()
            .map(asset -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("asset_id", asset.assetId());
                item.put("type", asset.type());
                item.put("url", asset.url());
                item.put("base64", asset.base64());
                item.put("mime_type", asset.mimeType());
                item.put("metadata", nullToEmpty(asset.metadata()));
                return item;
            })
            .toList();
    }

    private Map<String, Object> nullToEmpty(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }

    private <T extends KlingGenerationPayload> T requirePayload(
        KlingGenerationRequest<? extends KlingGenerationPayload> request,
        Class<T> payloadType
    ) {
        if (!payloadType.isInstance(request.payload())) {
            throw new IllegalArgumentException(
                request.generationType() + " requires payload type " + payloadType.getSimpleName()
            );
        }
        return payloadType.cast(request.payload());
    }

    @Override
    public Mono<Void> cancel(String backendTaskId) {
        return webClient.post()
            .uri(cancelPathTemplate, backendTaskId)
            .headers(headers -> headers.setBearerAuth(sign(accessKey, secretKey)))
            .retrieve()
            .bodyToMono(Void.class);
    }

    private String sign(String ak, String sk) {
        if (!StringUtils.hasText(ak) || !StringUtils.hasText(sk)) {
            throw new IllegalStateException("Kling API access-key and secret-key must be configured before calling Kling API");
        }
        try {
            long nowSeconds = Instant.now().getEpochSecond();
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payload = "{\"iss\":\"" + escapeJson(ak) + "\",\"exp\":" + (nowSeconds + 1800) + ",\"nbf\":" + (nowSeconds - 5) + "}";
            String signingInput = base64Url(header.getBytes(StandardCharsets.UTF_8))
                + "."
                + base64Url(payload.getBytes(StandardCharsets.UTF_8));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sk.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return signingInput + "." + base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign Kling API access token", ex);
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record KlingBackendSubmissionResponse(
        String backendTaskId,
        String backendProvider,
        String traceId
    ) {
    }

    private record BackendSubmitRequest(
        String path,
        Map<String, Object> body
    ) {
    }
}
