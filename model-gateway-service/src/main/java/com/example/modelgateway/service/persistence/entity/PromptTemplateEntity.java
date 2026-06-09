package com.example.modelgateway.service.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;

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

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getDefaultForScenario() {
        return defaultForScenario;
    }

    public void setDefaultForScenario(Boolean defaultForScenario) {
        this.defaultForScenario = defaultForScenario;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
