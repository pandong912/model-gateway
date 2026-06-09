package com.example.modelgateway.api.model;

import java.math.BigDecimal;

public record TokenUsage(
        long promptTokens,
        long completionTokens,
        long totalTokens,
        BigDecimal estimatedCost
) {
    public static TokenUsage empty() {
        return new TokenUsage(0, 0, 0, BigDecimal.ZERO);
    }
}
