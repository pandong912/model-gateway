package com.example.modelgateway.core.service;

import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.api.model.ChatStreamEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ModelInvocationService {
    Mono<ChatCompletionResponse> chat(ChatCompletionRequest request);

    Flux<ChatStreamEvent> stream(ChatCompletionRequest request);
}
