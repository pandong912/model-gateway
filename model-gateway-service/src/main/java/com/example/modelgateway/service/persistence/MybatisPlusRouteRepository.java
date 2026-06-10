package com.example.modelgateway.service.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.modelgateway.api.enums.ModelCapability;
import com.example.modelgateway.core.model.ModelRoute;
import com.example.modelgateway.core.route.RouteRepository;
import com.example.modelgateway.service.persistence.entity.ModelRouteEntity;
import com.example.modelgateway.service.persistence.mapper.ModelRouteMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MybatisPlusRouteRepository implements RouteRepository {
    private static final TypeReference<List<ModelCapability>> CAPABILITIES = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> METADATA = new TypeReference<>() {
    };

    private final ModelRouteMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<ModelRoute> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<ModelRouteEntity>()
                        .orderByAsc(ModelRouteEntity::getPriority)
                        .orderByAsc(ModelRouteEntity::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ModelRoute> findById(String id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public ModelRoute save(ModelRoute route) {
        ModelRouteEntity entity = toEntity(route);
        if (mapper.selectById(route.id()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return route;
    }

    private ModelRoute toDomain(ModelRouteEntity entity) {
        return new ModelRoute(
                entity.getId(),
                entity.getProvider(),
                entity.getModel(),
                readJson(entity.getCapabilities(), CAPABILITIES),
                readJson(entity.getScenarios(), STRING_LIST),
                entity.getPriority(),
                Boolean.TRUE.equals(entity.getEnabled()),
                readJson(entity.getFallbackRouteIds(), STRING_LIST),
                Duration.ofSeconds(entity.getTimeoutSeconds() == null ? 60 : entity.getTimeoutSeconds()),
                readJson(entity.getMetadata(), METADATA));
    }

    private ModelRouteEntity toEntity(ModelRoute route) {
        ModelRouteEntity entity = new ModelRouteEntity();
        entity.setId(route.id());
        entity.setProvider(route.provider());
        entity.setModel(route.model());
        entity.setCapabilities(writeJson(route.capabilities()));
        entity.setScenarios(writeJson(route.scenarios()));
        entity.setPriority(route.priority());
        entity.setEnabled(route.enabled());
        entity.setFallbackRouteIds(writeJson(route.fallbackRouteIds()));
        entity.setTimeoutSeconds(route.timeout() == null ? 60 : route.timeout().toSeconds());
        entity.setMetadata(writeJson(route.metadata()));
        return entity;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialize route value", ex);
        }
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to deserialize route value", ex);
        }
    }
}
