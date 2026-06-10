package com.example.modelgateway.ecommerce.api;

import java.util.List;

public record DisplayVideoPlan(
        String videoName,
        String prompt,
        String aspectRatio,
        int durationSeconds,
        List<String> referenceImages
) {
}
