package com.example.kling.inference.contract.model;

import com.example.kling.inference.contract.enums.GenerationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record KlingGenerationRequest<T extends KlingGenerationPayload>(
        @NotBlank String requestId,
        String idempotencyKey,
        @Valid InferenceCaller caller,
        @NotNull GenerationType generationType,
        String scenario,
        String model,
        String modelVersion,
        Integer priority,
        @Valid CallbackSpec callback,
        Map<String, Object> metadata,
        @Valid @NotNull T payload
) {
}
