package com.example.kling.inference.contract.model;

import java.util.List;
import java.util.Map;

public record KlingGenerationResult(
        List<OutputAsset> outputs,
        String coverUrl,
        String model,
        Integer seed,
        Map<String, Object> metadata
) {
}
