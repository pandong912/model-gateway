package com.example.kling.inference.service.persistence;

import com.example.kling.inference.contract.model.InferenceError;
import com.example.kling.inference.contract.model.VideoGenerationEvent;
import com.example.kling.inference.contract.model.VideoGenerationResult;
import com.example.kling.inference.service.persistence.entity.KlingInferenceEventEntity;
import com.example.kling.inference.service.persistence.mapper.KlingInferenceEventMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Repository
@RequiredArgsConstructor
public class KlingInferenceEventRepository {

    private final KlingInferenceEventMapper mapper;
    private final ObjectMapper objectMapper;

    public Mono<Void> save(VideoGenerationEvent event) {
        return Mono.fromRunnable(() -> mapper.insert(toEntity(event)))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private KlingInferenceEventEntity toEntity(VideoGenerationEvent event) {
        KlingInferenceEventEntity entity = new KlingInferenceEventEntity();
        entity.setEventId(event.eventId());
        entity.setJobId(event.jobId());
        entity.setEventType(event.type().name());
        entity.setStatus(event.status() == null ? null : event.status().name());
        entity.setProgress(event.progress());
        entity.setResultPayload(writeNullableJson(event.result()));
        entity.setErrorPayload(writeNullableJson(event.error()));
        entity.setMetadata(writeRequiredJson(event.metadata() == null ? Map.of() : event.metadata()));
        entity.setOccurredAt(event.occurredAt());
        return entity;
    }

    private String writeRequiredJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize kling inference event value", ex);
        }
    }

    private String writeNullableJson(Object value) {
        if (value == null) {
            return null;
        }
        return writeRequiredJson(value);
    }
}
