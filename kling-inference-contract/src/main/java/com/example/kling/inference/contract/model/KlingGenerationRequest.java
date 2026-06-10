package com.example.kling.inference.contract.model;

import com.example.kling.inference.contract.enums.GenerationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record KlingGenerationRequest(
        @NotBlank String requestId,
        String idempotencyKey,
        @Valid InferenceCaller caller,
        @NotNull GenerationType generationType,
        String scenario,
        String model,
        String modelVersion,
        @NotBlank String prompt,
        String negativePrompt,
        List<@Valid InputAsset> inputAssets,
        Integer durationSeconds,
        String aspectRatio,
        String resolution,
        Integer seed,
        Integer priority,
        @Valid CallbackSpec callback,
        Map<String, Object> parameters,
        Map<String, Object> metadata
) {
}
