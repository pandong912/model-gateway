package com.example.kling.inference.service.controller;

import com.example.kling.inference.contract.model.BackendTaskEvent;
import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.service.event.BackendTaskEventHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/v1/backend-events")
@RequiredArgsConstructor
public class BackendTaskEventController {

    private final BackendTaskEventHandler eventHandler;

    @PostMapping
    public Mono<KlingGenerationJob> accept(@Valid @RequestBody BackendTaskEvent event) {
        return eventHandler.handle(event);
    }
}
