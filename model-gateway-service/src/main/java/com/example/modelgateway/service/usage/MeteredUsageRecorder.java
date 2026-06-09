package com.example.modelgateway.service.usage;

import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.core.model.InvocationContext;
import com.example.modelgateway.core.model.ModelRoute;
import com.example.modelgateway.core.usage.UsageRecorder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

public class MeteredUsageRecorder implements UsageRecorder {
    private final MeterRegistry meterRegistry;

    public MeteredUsageRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordSuccess(ChatCompletionRequest request, ChatCompletionResponse response, ModelRoute route, InvocationContext context) {
        Tags tags = Tags.of(
                "tenant", tag(context.tenantId()),
                "provider", route.provider(),
                "model", route.model(),
                "route", route.id(),
                "scenario", tag(context.scenario()));
        Counter.builder("model.gateway.requests")
                .description("Successful model gateway requests")
                .tags(tags.and("outcome", "success"))
                .register(meterRegistry)
                .increment();
        if (response.usage() != null) {
            meterRegistry.counter("model.gateway.tokens", tags.and("type", "total"))
                    .increment(response.usage().totalTokens());
            meterRegistry.counter("model.gateway.tokens", tags.and("type", "prompt"))
                    .increment(response.usage().promptTokens());
            meterRegistry.counter("model.gateway.tokens", tags.and("type", "completion"))
                    .increment(response.usage().completionTokens());
        }
    }

    @Override
    public void recordFailure(ChatCompletionRequest request, ModelRoute route, InvocationContext context, Throwable error) {
        Counter.builder("model.gateway.requests")
                .description("Failed model gateway requests")
                .tags(
                        "tenant", tag(context.tenantId()),
                        "provider", route.provider(),
                        "model", route.model(),
                        "route", route.id(),
                        "scenario", tag(context.scenario()),
                        "outcome", "failure",
                        "exception", error.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();
    }

    private String tag(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
