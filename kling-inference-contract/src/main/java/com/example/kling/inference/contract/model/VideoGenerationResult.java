package com.example.kling.inference.contract.model;

import java.util.List;
import java.util.Map;

public record VideoGenerationResult(
        List<OutputAsset> outputs,
        String coverUrl,
        String model,
        String modelVersion,
        Integer seed,
        Map<String, Object> metadata
) {
}
