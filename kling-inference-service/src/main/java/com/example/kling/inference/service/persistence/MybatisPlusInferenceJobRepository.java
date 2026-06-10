package com.example.kling.inference.service.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.enums.InferenceJobStatus;
import com.example.kling.inference.contract.model.InferenceCaller;
import com.example.kling.inference.contract.model.InferenceError;
import com.example.kling.inference.contract.model.VideoGenerationJob;
import com.example.kling.inference.contract.model.VideoGenerationRequest;
import com.example.kling.inference.contract.model.VideoGenerationResult;
import com.example.kling.inference.core.InferenceJobRepository;
import com.example.kling.inference.service.persistence.entity.KlingInferenceJobEntity;
import com.example.kling.inference.service.persistence.mapper.KlingInferenceJobMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Repository
@RequiredArgsConstructor
public class MybatisPlusInferenceJobRepository implements InferenceJobRepository {

    private static final TypeReference<Map<String, Object>> METADATA = new TypeReference<>() {
    };

    private final KlingInferenceJobMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<VideoGenerationJob> save(VideoGenerationJob job, VideoGenerationRequest request) {
        return Mono.fromCallable(() -> {
            KlingInferenceJobEntity entity = toEntity(job, request);
            if (mapper.selectById(job.jobId()) == null) {
                mapper.insert(entity);
            } else {
                mapper.updateById(entity);
            }
            return job;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<VideoGenerationJob> update(VideoGenerationJob job) {
        return Mono.fromCallable(() -> {
            KlingInferenceJobEntity current = mapper.selectById(job.jobId());
            if (current == null) {
                throw new IllegalArgumentException("Kling inference job not found: " + job.jobId());
            }
            KlingInferenceJobEntity entity = toEntity(job, current);
            mapper.updateById(entity);
            return job;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<VideoGenerationJob> findById(String jobId) {
        return Mono.fromCallable(() -> mapper.selectById(jobId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(entity -> entity == null ? Mono.empty() : Mono.just(toDomain(entity)));
    }

    @Override
    public Mono<VideoGenerationJob> findByIdempotencyKey(String callerId, String idempotencyKey) {
        if (isBlank(callerId) || isBlank(idempotencyKey)) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> mapper.selectOne(new LambdaQueryWrapper<KlingInferenceJobEntity>()
                        .eq(KlingInferenceJobEntity::getCallerId, callerId)
                        .eq(KlingInferenceJobEntity::getIdempotencyKey, idempotencyKey)
                        .last("LIMIT 1")))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(entity -> entity == null ? Mono.empty() : Mono.just(toDomain(entity)));
    }

    @Override
    public Mono<VideoGenerationJob> findByBackendTaskId(String backendTaskId) {
        if (isBlank(backendTaskId)) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> mapper.selectOne(new LambdaQueryWrapper<KlingInferenceJobEntity>()
                        .eq(KlingInferenceJobEntity::getBackendTaskId, backendTaskId)
                        .last("LIMIT 1")))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(entity -> entity == null ? Mono.empty() : Mono.just(toDomain(entity)));
    }

    private KlingInferenceJobEntity toEntity(VideoGenerationJob job, VideoGenerationRequest request) {
        KlingInferenceJobEntity entity = new KlingInferenceJobEntity();
        InferenceCaller caller = request.caller();
        entity.setJobId(job.jobId());
        entity.setRequestId(job.requestId());
        entity.setIdempotencyKey(job.idempotencyKey());
        entity.setCallerId(job.callerId());
        entity.setCallerType(caller == null ? null : caller.callerType());
        entity.setTenantId(caller == null ? null : caller.tenantId());
        entity.setProjectId(caller == null ? null : caller.projectId());
        entity.setUserId(caller == null ? null : caller.userId());
        entity.setGenerationType(job.generationType().name());
        entity.setStatus(job.status().name());
        entity.setProgress(job.progress());
        entity.setBackendTaskId(job.backendTaskId());
        entity.setBackendProvider(job.backendProvider());
        entity.setTraceId(job.traceId());
        entity.setRequestPayload(writeRequiredJson(request));
        entity.setResultPayload(writeNullableJson(job.result()));
        entity.setErrorPayload(writeNullableJson(job.error()));
        entity.setMetadata(writeRequiredJson(job.metadata() == null ? Map.of() : job.metadata()));
        entity.setEstimatedWaitSeconds(job.estimatedWaitSeconds());
        entity.setCreatedAt(job.createdAt());
        entity.setUpdatedAt(job.updatedAt());
        entity.setExpiresAt(job.expiresAt());
        return entity;
    }

    private KlingInferenceJobEntity toEntity(VideoGenerationJob job, KlingInferenceJobEntity current) {
        current.setRequestId(job.requestId());
        current.setIdempotencyKey(job.idempotencyKey());
        current.setCallerId(job.callerId());
        current.setGenerationType(job.generationType().name());
        current.setStatus(job.status().name());
        current.setProgress(job.progress());
        current.setBackendTaskId(job.backendTaskId());
        current.setBackendProvider(job.backendProvider());
        current.setTraceId(job.traceId());
        current.setResultPayload(writeNullableJson(job.result()));
        current.setErrorPayload(writeNullableJson(job.error()));
        current.setMetadata(writeRequiredJson(job.metadata() == null ? Map.of() : job.metadata()));
        current.setEstimatedWaitSeconds(job.estimatedWaitSeconds());
        current.setCreatedAt(job.createdAt());
        current.setUpdatedAt(job.updatedAt());
        current.setExpiresAt(job.expiresAt());
        return current;
    }

    private VideoGenerationJob toDomain(KlingInferenceJobEntity entity) {
        return new VideoGenerationJob(
                entity.getJobId(),
                entity.getRequestId(),
                entity.getIdempotencyKey(),
                entity.getCallerId(),
                GenerationType.valueOf(entity.getGenerationType()),
                InferenceJobStatus.valueOf(entity.getStatus()),
                entity.getProgress(),
                entity.getBackendTaskId(),
                entity.getBackendProvider(),
                entity.getTraceId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getExpiresAt(),
                entity.getEstimatedWaitSeconds(),
                readNullableJson(entity.getResultPayload(), VideoGenerationResult.class),
                readNullableJson(entity.getErrorPayload(), InferenceError.class),
                readRequiredJson(entity.getMetadata(), METADATA)
        );
    }

    private String writeRequiredJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize kling inference job value", ex);
        }
    }

    private String writeNullableJson(Object value) {
        if (value == null) {
            return null;
        }
        return writeRequiredJson(value);
    }

    private <T> T readRequiredJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to deserialize kling inference job value", ex);
        }
    }

    private <T> T readNullableJson(String json, Class<T> type) {
        if (isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to deserialize kling inference job value", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
