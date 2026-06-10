package com.example.kling.inference.contract.model;

import java.util.Map;

public record CancelJobRequest(
        String reason,
        String operator,
        Map<String, Object> metadata
) {
}
