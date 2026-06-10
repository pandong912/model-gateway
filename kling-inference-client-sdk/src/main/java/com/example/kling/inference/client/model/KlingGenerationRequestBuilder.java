package com.example.kling.inference.client.model;

import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.model.CallbackSpec;
import com.example.kling.inference.contract.model.ImageGenerationPayload;
import com.example.kling.inference.contract.model.InferenceCaller;
import com.example.kling.inference.contract.model.InputAsset;
import com.example.kling.inference.contract.model.KlingGenerationPayload;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.VideoGenerationPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KlingGenerationRequestBuilder {

    private String requestId = "req_" + UUID.randomUUID().toString().replace("-", "");
    private String idempotencyKey;
    private InferenceCaller caller;
    private GenerationType generationType = GenerationType.TEXT_TO_VIDEO;
    private String scenario;
    private String model = "kling-video";
    private String modelVersion;
    private String prompt;
    private String negativePrompt;
    private final List<InputAsset> inputAssets = new ArrayList<>();
    private Integer durationSeconds;
    private String aspectRatio;
    private String resolution;
    private Integer seed;
    private Integer priority;
    private CallbackSpec callback;
    private Map<String, Object> parameters = Map.of();
    private Map<String, Object> metadata = Map.of();

    public static KlingGenerationRequestBuilder textToVideo(String prompt) {
        return new KlingGenerationRequestBuilder()
                .generationType(GenerationType.TEXT_TO_VIDEO)
                .model("kling-v3")
                .prompt(prompt);
    }

    public static KlingGenerationRequestBuilder imageToVideo(String prompt, InputAsset image) {
        return new KlingGenerationRequestBuilder()
                .generationType(GenerationType.IMAGE_TO_VIDEO)
                .prompt(prompt)
                .addInputAsset(image);
    }

    public static KlingGenerationRequestBuilder imageGeneration(String prompt) {
        return new KlingGenerationRequestBuilder()
                .generationType(GenerationType.IMAGE_GENERATION)
                .model("kling-v3")
                .prompt(prompt);
    }

    public static KlingGenerationRequestBuilder imageEditing(String prompt, InputAsset image) {
        return new KlingGenerationRequestBuilder()
                .generationType(GenerationType.IMAGE_EDITING)
                .model("kling-image")
                .prompt(prompt)
                .addInputAsset(image);
    }

    public KlingGenerationRequestBuilder requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public KlingGenerationRequestBuilder idempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        return this;
    }

    public KlingGenerationRequestBuilder caller(InferenceCaller caller) {
        this.caller = caller;
        return this;
    }

    public KlingGenerationRequestBuilder generationType(GenerationType generationType) {
        this.generationType = generationType;
        return this;
    }

    public KlingGenerationRequestBuilder scenario(String scenario) {
        this.scenario = scenario;
        return this;
    }

    public KlingGenerationRequestBuilder model(String model) {
        this.model = model;
        return this;
    }

    public KlingGenerationRequestBuilder modelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
        return this;
    }

    public KlingGenerationRequestBuilder prompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public KlingGenerationRequestBuilder negativePrompt(String negativePrompt) {
        this.negativePrompt = negativePrompt;
        return this;
    }

    public KlingGenerationRequestBuilder addInputAsset(InputAsset inputAsset) {
        this.inputAssets.add(inputAsset);
        return this;
    }

    public KlingGenerationRequestBuilder durationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
        return this;
    }

    public KlingGenerationRequestBuilder aspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
        return this;
    }

    public KlingGenerationRequestBuilder resolution(String resolution) {
        this.resolution = resolution;
        return this;
    }

    public KlingGenerationRequestBuilder seed(Integer seed) {
        this.seed = seed;
        return this;
    }

    public KlingGenerationRequestBuilder priority(Integer priority) {
        this.priority = priority;
        return this;
    }

    public KlingGenerationRequestBuilder callback(CallbackSpec callback) {
        this.callback = callback;
        return this;
    }

    public KlingGenerationRequestBuilder parameters(Map<String, Object> parameters) {
        this.parameters = parameters == null ? Map.of() : parameters;
        return this;
    }

    public KlingGenerationRequestBuilder metadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? Map.of() : metadata;
        return this;
    }

    public KlingGenerationRequest<? extends KlingGenerationPayload> build() {
        KlingGenerationPayload payload = buildPayload();
        return new KlingGenerationRequest<>(
                requestId,
                idempotencyKey,
                caller,
                generationType,
                scenario,
                model,
                priority,
                callback,
                metadata,
                payload
        );
    }

    private KlingGenerationPayload buildPayload() {
        if (generationType == GenerationType.IMAGE_GENERATION || generationType == GenerationType.IMAGE_EDITING) {
            return new ImageGenerationPayload(
                    prompt,
                    negativePrompt,
                    List.copyOf(inputAssets),
                    resolution,
                    seed,
                    parameters
            );
        }
        return new VideoGenerationPayload(
                prompt,
                negativePrompt,
                List.copyOf(inputAssets),
                durationSeconds,
                aspectRatio,
                resolution,
                seed,
                parameters
        );
    }
}
