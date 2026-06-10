package com.example.kling.inference.service.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("kling_inference_events")
public class KlingInferenceEventEntity {
    @TableId(type = IdType.INPUT)
    private String eventId;
    private String jobId;
    private String eventType;
    private String status;
    private Integer progress;
    private String resultPayload;
    private String errorPayload;
    private String metadata;
    private Instant occurredAt;
}
