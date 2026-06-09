package com.example.modelgateway.api.model;

import com.example.modelgateway.api.enums.GatewayErrorCode;
import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        GatewayErrorCode code,
        String message,
        String traceId,
        Instant timestamp,
        Map<String, Object> details
) {
    public static ErrorResponse of(GatewayErrorCode code, String message, String traceId) {
        return new ErrorResponse(code, message, traceId, Instant.now(), Map.of());
    }
}
