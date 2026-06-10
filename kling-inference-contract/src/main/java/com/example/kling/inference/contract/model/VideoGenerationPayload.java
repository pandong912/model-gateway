package com.example.kling.inference.contract.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public record VideoGenerationPayload(
        @NotBlank String prompt,
        String negativePrompt,
        List<@Valid InputAsset> inputAssets,
        Integer durationSeconds,
        String aspectRatio,
        String mode,
        Integer seed,
        Map<String, Object> parameters
) implements KlingGenerationPayload {
}
