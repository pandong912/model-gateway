package com.example.modelgateway.ecommerce.api;

import java.util.List;
import java.util.Map;

public record VideoResult(
        String videoName,
        String url,
        String providerJobId,
        List<String> referenceImages,
        Map<String, Object> metadata
) {
}
