package com.example.kling.inference.service.controller;

import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.contract.model.KlingGenerationResult;
import com.example.kling.inference.core.InferenceOrchestrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/v1/generation-jobs")
public class GenerationJobController {

    private final InferenceOrchestrationService orchestrationService;

    public GenerationJobController(InferenceOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @GetMapping("/{jobId}")
    public Mono<KlingGenerationJob> getJob(@PathVariable String jobId) {
        return orchestrationService.getJob(jobId);
    }

    @GetMapping("/{jobId}/result")
    public Mono<KlingGenerationResult> getResult(@PathVariable String jobId) {
        return orchestrationService.getResult(jobId);
    }
}
