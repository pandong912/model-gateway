package com.example.modelgateway.api.model;

import com.example.modelgateway.api.enums.MessageRole;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record ChatMessage(
        @NotNull MessageRole role,
        String content,
        String name,
        List<MediaContent> media,
        Map<String, Object> metadata
) {
    public static ChatMessage system(String content) {
        return new ChatMessage(MessageRole.SYSTEM, content, null, List.of(), Map.of());
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(MessageRole.USER, content, null, List.of(), Map.of());
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(MessageRole.ASSISTANT, content, null, List.of(), Map.of());
    }
}
