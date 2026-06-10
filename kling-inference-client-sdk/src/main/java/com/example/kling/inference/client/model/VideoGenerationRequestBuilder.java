package com.example.kling.inference.client.model;

import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.model.CallbackSpec;
import com.example.kling.inference.contract.model.InferenceCaller;
import com.example.kling.inference.contract.model.InputAsset;
import com.example.kling.inference.contract.model.VideoGenerationRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VideoGenerationRequestBuilder {

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

    public static VideoGenerationRequestBuilder textToVideo(String prompt) {
        return new VideoGenerationRequestBuilder()
                .generationType(GenerationType.TEXT_TO_VIDEO)
                .prompt(prompt);
    }

    public static VideoGenerationRequestBuilder imageToVideo(String prompt, InputAsset image) {
        return new VideoGenerationRequestBuilder()
                .generationType(GenerationType.IMAGE_TO_VIDEO)
                .prompt(prompt)
                .addInputAsset(image);
    }

    public VideoGenerationRequestBuilder requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public VideoGenerationRequestBuilder idempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
        return this;
    }

    public VideoGenerationRequestBuilder caller(InferenceCaller caller) {
        this.caller = caller;
        return this;
    }

    public VideoGenerationRequestBuilder generationType(GenerationType generationType) {
        this.generationType = generationType;
        return this;
    }

    public VideoGenerationRequestBuilder scenario(String scenario) {
        this.scenario = scenario;
        return this;
    }

    public VideoGenerationRequestBuilder model(String model) {
        this.model = model;
        return this;
    }

    public VideoGenerationRequestBuilder modelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
        return this;
    }

    public VideoGenerationRequestBuilder prompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public VideoGenerationRequestBuilder negativePrompt(String negativePrompt) {
        this.negativePrompt = negativePrompt;
        return this;
    }

    public VideoGenerationRequestBuilder addInputAsset(InputAsset inputAsset) {
        this.inputAssets.add(inputAsset);
        return this;
    }

    public VideoGenerationRequestBuilder durationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
        return this;
    }

    public VideoGenerationRequestBuilder aspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
        return this;
    }

    public VideoGenerationRequestBuilder resolution(String resolution) {
        this.resolution = resolution;
        return this;
    }

    public VideoGenerationRequestBuilder seed(Integer seed) {
        this.seed = seed;
        return this;
    }

    public VideoGenerationRequestBuilder priority(Integer priority) {
        this.priority = priority;
        return this;
    }

    public VideoGenerationRequestBuilder callback(CallbackSpec callback) {
        this.callback = callback;
        return this;
    }

    public VideoGenerationRequestBuilder parameters(Map<String, Object> parameters) {
        this.parameters = parameters == null ? Map.of() : parameters;
        return this;
    }

    public VideoGenerationRequestBuilder metadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? Map.of() : metadata;
        return this;
    }

    public VideoGenerationRequest build() {
        return new VideoGenerationRequest(
                requestId,
                idempotencyKey,
                caller,
                generationType,
                scenario,
                model,
                modelVersion,
                prompt,
                negativePrompt,
                List.copyOf(inputAssets),
                durationSeconds,
                aspectRatio,
                resolution,
                seed,
                priority,
                callback,
                parameters,
                metadata
        );
    }
}
