package com.example.modelgateway.client;

import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.api.model.ChatStreamEvent;
import com.example.modelgateway.core.model.PromptTemplate;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ModelGatewayClient {
    Mono<ChatCompletionResponse> chat(ChatCompletionRequest request);

    Flux<ChatStreamEvent> stream(ChatCompletionRequest request);

    Mono<List<PromptTemplate>> prompts();

    Mono<PromptTemplate> savePrompt(PromptTemplate promptTemplate);
}
