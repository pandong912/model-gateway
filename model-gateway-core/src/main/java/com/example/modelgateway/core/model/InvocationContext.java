package com.example.modelgateway.core.model;

import java.util.UUID;

public record InvocationContext(
        String traceId,
        String tenantId,
        String projectId,
        String scenario
) {
    public static InvocationContext create(String tenantId, String projectId, String scenario) {
        return new InvocationContext(UUID.randomUUID().toString(), tenantId, projectId, scenario);
    }
}
