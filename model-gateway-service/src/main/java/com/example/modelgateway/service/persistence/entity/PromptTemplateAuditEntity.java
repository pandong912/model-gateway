package com.example.modelgateway.service.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("prompt_template_audit")
public class PromptTemplateAuditEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String promptKey;
    private String version;
    private String action;
    private String snapshotJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPromptKey() {
        return promptKey;
    }

    public void setPromptKey(String promptKey) {
        this.promptKey = promptKey;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }
}
