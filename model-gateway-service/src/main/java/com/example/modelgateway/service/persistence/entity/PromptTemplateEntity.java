package com.example.modelgateway.service.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("prompt_templates")
public class PromptTemplateEntity {
    private String promptKey;
    private String version;
    private String scenario;
    private String locale;
    private String role;
    private String content;
    private Boolean enabled;
    private Boolean defaultForScenario;
    private String metadata;
}
