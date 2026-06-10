package com.example.modelgateway.core.service;

import com.example.modelgateway.api.enums.GatewayErrorCode;
import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.api.model.ChatStreamEvent;
import com.example.modelgateway.core.exception.GatewayException;
import com.example.modelgateway.core.model.InvocationContext;
import com.example.modelgateway.core.model.ModelRoute;
import com.example.modelgateway.core.provider.ProviderRegistry;
import com.example.modelgateway.core.quota.QuotaService;
import com.example.modelgateway.core.route.ModelRouter;
import com.example.modelgateway.core.route.RouteRepository;
import com.example.modelgateway.core.usage.UsageRecorder;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class DefaultModelInvocationService implements ModelInvocationService {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private final ModelRouter router;
    private final RouteRepository routeRepository;
    private final ProviderRegistry providerRegistry;
    private final QuotaService quotaService;
    private final UsageRecorder usageRecorder;
    private final PromptTemplateService promptTemplateService;
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    @Override
    public Mono<ChatCompletionResponse> chat(ChatCompletionRequest request) {
        InvocationContext context = InvocationContext.create(request.tenantId(), request.projectId(), request.scenario());
        ModelRoute route = router.route(request);
        ChatCompletionRequest enrichedRequest = promptTemplateService.apply(request, route);
        return invokeChat(enrichedRequest, route, context, new HashSet<>());
    }

    @Override
    public Flux<ChatStreamEvent> stream(ChatCompletionRequest request) {
        InvocationContext context = InvocationContext.create(request.tenantId(), request.projectId(), request.scenario());
        ModelRoute route = router.route(request);
        ChatCompletionRequest enrichedRequest = promptTemplateService.apply(request, route);
        return invokeStream(enrichedRequest, route, context, new HashSet<>());
    }

    private Mono<ChatCompletionResponse> invokeChat(
            ChatCompletionRequest request,
            ModelRoute route,
            InvocationContext context,
            Set<String> attemptedRoutes
    ) {
        attemptedRoutes.add(route.id());
        quotaService.check(request, route, context);
        return providerRegistry.getRequired(route.provider())
                .chat(request, route, context)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker(route)))
                .timeout(route.timeout() == null ? DEFAULT_TIMEOUT : route.timeout())
                .doOnSuccess(response -> usageRecorder.recordSuccess(request, response, route, context))
                .doOnError(error -> usageRecorder.recordFailure(request, route, context, error))
                .onErrorResume(error -> fallbackChat(request, route, context, attemptedRoutes, error));
    }

    private Flux<ChatStreamEvent> invokeStream(
            ChatCompletionRequest request,
            ModelRoute route,
            InvocationContext context,
            Set<String> attemptedRoutes
    ) {
        attemptedRoutes.add(route.id());
        quotaService.check(request, route, context);
        return providerRegistry.getRequired(route.provider())
                .stream(request, route, context)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker(route)))
                .timeout(route.timeout() == null ? DEFAULT_TIMEOUT : route.timeout())
                .onErrorResume(error -> fallbackStream(request, route, context, attemptedRoutes, error));
    }

    private Mono<ChatCompletionResponse> fallbackChat(
            ChatCompletionRequest request,
            ModelRoute route,
            InvocationContext context,
            Set<String> attemptedRoutes,
            Throwable error
    ) {
        return nextFallback(route, attemptedRoutes)
                .map(nextRoute -> invokeChat(request, nextRoute, context, attemptedRoutes))
                .orElseGet(() -> Mono.error(normalize(error)));
    }

    private Flux<ChatStreamEvent> fallbackStream(
            ChatCompletionRequest request,
            ModelRoute route,
            InvocationContext context,
            Set<String> attemptedRoutes,
            Throwable error
    ) {
        return nextFallback(route, attemptedRoutes)
                .map(nextRoute -> invokeStream(request, nextRoute, context, attemptedRoutes))
                .orElseGet(() -> Flux.error(normalize(error)));
    }

    private java.util.Optional<ModelRoute> nextFallback(ModelRoute route, Set<String> attemptedRoutes) {
        return route.fallbackRouteIds().stream()
                .filter(routeId -> !attemptedRoutes.contains(routeId))
                .map(routeRepository::findById)
                .flatMap(java.util.Optional::stream)
                .filter(ModelRoute::enabled)
                .findFirst();
    }

    private Throwable normalize(Throwable error) {
        if (error instanceof GatewayException) {
            return error;
        }
        if (error instanceof TimeoutException) {
            return new GatewayException(GatewayErrorCode.PROVIDER_TIMEOUT, "Model provider timed out", 504, error);
        }
        if (isWebClientResponseException(error)) {
            String detail = webClientResponseDetail(error);
            return new GatewayException(
                    GatewayErrorCode.PROVIDER_UNAVAILABLE,
                    "Model provider invocation failed: " + detail,
                    502,
                    error);
        }
        return new GatewayException(GatewayErrorCode.PROVIDER_UNAVAILABLE, "Model provider invocation failed", 502, error);
    }

    private boolean isWebClientResponseException(Throwable error) {
        return "org.springframework.web.reactive.function.client.WebClientResponseException"
                .equals(error.getClass().getName());
    }

    private String webClientResponseDetail(Throwable error) {
        try {
            Object statusCode = error.getClass().getMethod("getStatusCode").invoke(error);
            Object responseBody = error.getClass().getMethod("getResponseBodyAsString").invoke(error);
            String body = responseBody == null ? "" : String.valueOf(responseBody);
            return body.isBlank() ? "HTTP " + statusCode : "HTTP " + statusCode + " " + body;
        } catch (Exception reflectionError) {
            return error.getMessage();
        }
    }

    private CircuitBreaker circuitBreaker(ModelRoute route) {
        return circuitBreakers.computeIfAbsent(route.id(), routeId -> CircuitBreaker.of(
                "model-route-" + routeId,
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .minimumNumberOfCalls(10)
                        .slidingWindowSize(20)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .build()));
    }
}
