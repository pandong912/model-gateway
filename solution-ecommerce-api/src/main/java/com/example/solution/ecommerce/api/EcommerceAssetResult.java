package com.example.solution.ecommerce.api;

import java.util.List;
import java.util.Map;

public record EcommerceAssetResult(
        String jobId,
        EcommerceAssetJobStatus status,
        EcommerceAssetPlan plan,
        ImageResult modelImage,
        List<ImageResult> displayImages,
        VideoResult displayVideo,
        Map<String, Object> metadata
) {
}
