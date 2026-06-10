package com.example.solution.ecommerce.api;

import java.util.List;

public record ModelInfo(
        boolean required,
        boolean userProvided,
        String imageName,
        String prompt,
        String description,
        List<String> referenceImages
) {
    public static ModelInfo empty() {
        return new ModelInfo(false, false, null, null, null, List.of());
    }
}
