package com.example.solution.ecommerce.api;

import java.util.Map;

public record AssetInput(
        String name,
        String url,
        String base64,
        String mimeType,
        Map<String, Object> metadata
) {
}
