package com.example.kling.inference.service.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("kling_inference_jobs")
public class KlingInferenceJobEntity {
    @TableId(type = IdType.INPUT)
    private String jobId;
    private String requestId;
    private String idempotencyKey;
    private String callerId;
    private String callerType;
    private String tenantId;
    private String projectId;
    private String userId;
    private String generationType;
    private String status;
    private Integer progress;
    private String backendTaskId;
    private String backendProvider;
    private String traceId;
    private String requestPayload;
    private String resultPayload;
    private String errorPayload;
    private String metadata;
    private Integer estimatedWaitSeconds;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
}
