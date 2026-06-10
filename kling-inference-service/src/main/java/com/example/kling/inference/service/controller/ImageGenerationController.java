package com.example.kling.inference.service.controller;

import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.model.CancelJobRequest;
import com.example.kling.inference.contract.model.VideoGenerationEvent;
import com.example.kling.inference.contract.model.VideoGenerationJob;
import com.example.kling.inference.contract.model.VideoGenerationRequest;
import com.example.kling.inference.core.InferenceOrchestrationService;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/internal/v1/image-generations")
public class ImageGenerationController {

    private static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration MAX_WAIT_TIMEOUT = Duration.ofSeconds(120);

    private final InferenceOrchestrationService orchestrationService;

    public ImageGenerationController(InferenceOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping
    public Mono<ResponseEntity<VideoGenerationJob>> submit(
            @Valid @RequestBody VideoGenerationRequest request,
            @RequestParam(name = "wait", defaultValue = "false") boolean wait,
            @RequestParam(name = "timeoutSeconds", required = false) Long timeoutSeconds
    ) {
        validateImageGenerationRequest(request);
        Mono<VideoGenerationJob> jobMono = orchestrationService.submit(request)
                .flatMap(job -> wait
                        ? orchestrationService.waitJob(job.jobId(), normalizeTimeout(timeoutSeconds))
                        : Mono.just(job));

        return jobMono.map(job -> ResponseEntity
                .status(job.status().isTerminal() ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(job));
    }

    @GetMapping("/{jobId}")
    public Mono<VideoGenerationJob> getJob(@PathVariable String jobId) {
        return orchestrationService.getJob(jobId);
    }

    @GetMapping(path = {"/{jobId}:wait", "/{jobId}/wait"})
    public Mono<VideoGenerationJob> waitJob(
            @PathVariable String jobId,
            @RequestParam(name = "timeoutSeconds", required = false) Long timeoutSeconds
    ) {
        return orchestrationService.waitJob(jobId, normalizeTimeout(timeoutSeconds));
    }

    @GetMapping(path = "/{jobId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<VideoGenerationEvent> watchJob(@PathVariable String jobId) {
        return orchestrationService.watchJob(jobId);
    }

    @PostMapping(path = {"/{jobId}:cancel", "/{jobId}/cancel"})
    public Mono<VideoGenerationJob> cancelJob(
            @PathVariable String jobId,
            @RequestBody(required = false) CancelJobRequest request
    ) {
        return orchestrationService.cancelJob(jobId, request == null ? new CancelJobRequest(null, null, null) : request);
    }

    private void validateImageGenerationRequest(VideoGenerationRequest request) {
        if (request.generationType() != GenerationType.IMAGE_GENERATION
                && request.generationType() != GenerationType.IMAGE_EDITING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image generation API only accepts IMAGE_GENERATION or IMAGE_EDITING generationType"
            );
        }
    }

    private Duration normalizeTimeout(Long timeoutSeconds) {
        if (timeoutSeconds == null || timeoutSeconds <= 0) {
            return DEFAULT_WAIT_TIMEOUT;
        }
        Duration requested = Duration.ofSeconds(timeoutSeconds);
        return requested.compareTo(MAX_WAIT_TIMEOUT) > 0 ? MAX_WAIT_TIMEOUT : requested;
    }
}
