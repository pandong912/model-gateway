package com.example.kling.inference.contract.model;

import com.example.kling.inference.contract.enums.InferenceEventType;
import java.util.List;
import java.util.Map;

public record CallbackSpec(
        String url,
        List<InferenceEventType> events,
        String secretRef,
        Map<String, Object> metadata
) {
}
