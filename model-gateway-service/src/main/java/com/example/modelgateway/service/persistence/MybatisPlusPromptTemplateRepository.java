package com.example.modelgateway.service.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.modelgateway.api.enums.MessageRole;
import com.example.modelgateway.core.model.PromptTemplate;
import com.example.modelgateway.core.service.PromptTemplateRepository;
import com.example.modelgateway.service.persistence.entity.PromptTemplateAuditEntity;
import com.example.modelgateway.service.persistence.entity.PromptTemplateEntity;
import com.example.modelgateway.service.persistence.mapper.PromptTemplateAuditMapper;
import com.example.modelgateway.service.persistence.mapper.PromptTemplateMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MybatisPlusPromptTemplateRepository implements PromptTemplateRepository {
    private static final TypeReference<Map<String, Object>> METADATA = new TypeReference<>() {
    };

    private final PromptTemplateMapper mapper;
    private final PromptTemplateAuditMapper auditMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<PromptTemplate> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<PromptTemplateEntity>()
                        .orderByAsc(PromptTemplateEntity::getPromptKey)
                        .orderByAsc(PromptTemplateEntity::getVersion))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<PromptTemplate> find(String key, String version) {
        return Optional.ofNullable(mapper.selectOne(baseKeyVersionQuery(key, version)
                        .eq(PromptTemplateEntity::getEnabled, true)))
                .map(this::toDomain);
    }

    @Override
    public Optional<PromptTemplate> findLatest(String key) {
        return mapper.selectList(new LambdaQueryWrapper<PromptTemplateEntity>()
                        .eq(PromptTemplateEntity::getPromptKey, key)
                        .eq(PromptTemplateEntity::getEnabled, true)
                        .orderByDesc(PromptTemplateEntity::getVersion)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public Optional<PromptTemplate> findDefaultForScenario(String scenario, String locale) {
        if (scenario == null || scenario.isBlank()) {
            return Optional.empty();
        }
        LambdaQueryWrapper<PromptTemplateEntity> query = new LambdaQueryWrapper<PromptTemplateEntity>()
                .eq(PromptTemplateEntity::getScenario, scenario)
                .eq(PromptTemplateEntity::getEnabled, true)
                .eq(PromptTemplateEntity::getDefaultForScenario, true)
                .orderByDesc(PromptTemplateEntity::getVersion)
                .last("LIMIT 1");
        if (locale != null && !locale.isBlank()) {
            query.and(wrapper -> wrapper.isNull(PromptTemplateEntity::getLocale)
                    .or()
                    .eq(PromptTemplateEntity::getLocale, locale));
        }
        return mapper.selectList(query).stream().findFirst().map(this::toDomain);
    }

    @Override
    public PromptTemplate save(PromptTemplate template) {
        PromptTemplateEntity entity = toEntity(template);
        if (findExisting(template.key(), template.version()).isPresent()) {
            mapper.update(entity, new LambdaUpdateWrapper<PromptTemplateEntity>()
                    .eq(PromptTemplateEntity::getPromptKey, template.key())
                    .eq(PromptTemplateEntity::getVersion, template.version()));
        } else {
            mapper.insert(entity);
        }
        recordAudit(template);
        return template;
    }

    private Optional<PromptTemplateEntity> findExisting(String key, String version) {
        return Optional.ofNullable(mapper.selectOne(baseKeyVersionQuery(key, version)));
    }

    private LambdaQueryWrapper<PromptTemplateEntity> baseKeyVersionQuery(String key, String version) {
        return new LambdaQueryWrapper<PromptTemplateEntity>()
                .eq(PromptTemplateEntity::getPromptKey, key)
                .eq(PromptTemplateEntity::getVersion, version);
    }

    private PromptTemplate toDomain(PromptTemplateEntity entity) {
        return new PromptTemplate(
                entity.getPromptKey(),
                entity.getVersion(),
                entity.getScenario(),
                entity.getLocale(),
                MessageRole.valueOf(entity.getRole()),
                entity.getContent(),
                Boolean.TRUE.equals(entity.getEnabled()),
                Boolean.TRUE.equals(entity.getDefaultForScenario()),
                readJson(entity.getMetadata()));
    }

    private PromptTemplateEntity toEntity(PromptTemplate template) {
        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.setPromptKey(template.key());
        entity.setVersion(template.version());
        entity.setScenario(template.scenario());
        entity.setLocale(template.locale());
        entity.setRole((template.role() == null ? MessageRole.SYSTEM : template.role()).name());
        entity.setContent(template.content());
        entity.setEnabled(template.enabled());
        entity.setDefaultForScenario(template.defaultForScenario());
        entity.setMetadata(writeJson(template.metadata()));
        return entity;
    }

    private void recordAudit(PromptTemplate template) {
        PromptTemplateAuditEntity audit = new PromptTemplateAuditEntity();
        audit.setPromptKey(template.key());
        audit.setVersion(template.version());
        audit.setAction("UPSERT");
        audit.setSnapshotJson(writeJson(template));
        auditMapper.insert(audit);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialize prompt template", ex);
        }
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, METADATA);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to deserialize prompt template metadata", ex);
        }
    }
}
