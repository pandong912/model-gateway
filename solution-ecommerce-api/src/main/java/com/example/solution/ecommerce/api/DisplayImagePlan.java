package com.example.solution.ecommerce.api;

import java.util.List;

public record DisplayImagePlan(
        int id,
        String imageName,
        String content,
        String prompt,
        String aspectRatio,
        List<String> referenceImages
) {
}
