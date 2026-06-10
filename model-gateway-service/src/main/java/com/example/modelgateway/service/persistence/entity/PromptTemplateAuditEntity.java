package com.example.modelgateway.service.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("prompt_template_audit")
public class PromptTemplateAuditEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String promptKey;
    private String version;
    private String action;
    private String snapshotJson;
}
