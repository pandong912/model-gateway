package com.example.kling.inference.contract.model;

import java.util.Map;

public record InferenceError(
        String code,
        String message,
        boolean retryable,
        Map<String, Object> details
) {
}
