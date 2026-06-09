package com.example.modelgateway.service.controller;

import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.api.model.ChatStreamEvent;
import com.example.modelgateway.core.service.ModelInvocationService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ModelInvocationService invocationService;

    public ChatController(ModelInvocationService invocationService) {
        this.invocationService = invocationService;
    }

    @PostMapping("/completions")
    public Mono<ChatCompletionResponse> completions(@Valid @RequestBody ChatCompletionRequest request) {
        return invocationService.chat(request);
    }

    @PostMapping(value = "/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ChatStreamEvent>> stream(@Valid @RequestBody ChatCompletionRequest request) {
        return invocationService.stream(request)
                .map(event -> ServerSentEvent.builder(event)
                        .event(event.type())
                        .id(event.id())
                        .build());
    }
}
