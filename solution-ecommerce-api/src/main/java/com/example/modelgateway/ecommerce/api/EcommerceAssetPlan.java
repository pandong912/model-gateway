package com.example.modelgateway.ecommerce.api;

import java.util.List;
import java.util.Map;

public record EcommerceAssetPlan(
        String visualDna,
        ModelInfo modelInfo,
        List<DisplayImagePlan> displayImages,
        DisplayVideoPlan displayVideo,
        Map<String, Object> metadata
) {
}
