package com.example.solution.ecommerce.api;

import java.util.List;
import java.util.Map;

public record EcommerceAssetRequest(
        String tenantId,
        String projectId,
        String productInfo,
        List<AssetInput> productImages,
        List<AssetInput> modelImages,
        Map<String, Object> metadata
) {
}
