package com.example.kling.inference.contract.model;

import java.util.Map;

public record InferenceCaller(
        String callerId,
        String callerType,
        String tenantId,
        String projectId,
        String userId,
        Map<String, Object> metadata
) {
}
