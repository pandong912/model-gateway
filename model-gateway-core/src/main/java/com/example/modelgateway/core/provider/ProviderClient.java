package com.example.modelgateway.core.provider;

import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.api.model.ChatStreamEvent;
import com.example.modelgateway.core.model.InvocationContext;
import com.example.modelgateway.core.model.ModelRoute;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProviderClient {
    String providerId();

    Mono<ChatCompletionResponse> chat(ChatCompletionRequest request, ModelRoute route, InvocationContext context);

    Flux<ChatStreamEvent> stream(ChatCompletionRequest request, ModelRoute route, InvocationContext context);
}
