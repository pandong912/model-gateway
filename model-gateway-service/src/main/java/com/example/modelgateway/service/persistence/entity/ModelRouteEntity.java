package com.example.modelgateway.service.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("model_routes")
public class ModelRouteEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    private String provider;
    private String model;
    private String capabilities;
    private String scenarios;
    private Integer priority;
    private Boolean enabled;
    private String fallbackRouteIds;
    private Long timeoutSeconds;
    private String metadata;
}
