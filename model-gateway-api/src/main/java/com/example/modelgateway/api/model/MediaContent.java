package com.example.modelgateway.api.model;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record MediaContent(
        @NotBlank String type,
        String url,
        String base64,
        String mimeType,
        Map<String, Object> metadata
) {
}
