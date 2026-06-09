package com.example.modelgateway.api.model;

import java.util.Map;

public record GeneratedMedia(
        String type,
        String mimeType,
        String base64,
        String url,
        Map<String, Object> metadata
) {
    public static GeneratedMedia image(String mimeType, String base64, Map<String, Object> metadata) {
        return new GeneratedMedia("image", mimeType, base64, null, metadata == null ? Map.of() : metadata);
    }
}
