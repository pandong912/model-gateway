package com.example.modelgateway.service.usage;

import com.example.modelgateway.api.enums.GatewayErrorCode;
import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.core.exception.GatewayException;
import com.example.modelgateway.core.model.InvocationContext;
import com.example.modelgateway.core.model.ModelRoute;
import com.example.modelgateway.core.quota.QuotaService;
import com.example.modelgateway.service.config.ModelGatewayProperties;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SlidingWindowQuotaService implements QuotaService {
    private final ModelGatewayProperties.Quota quota;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public SlidingWindowQuotaService(ModelGatewayProperties.Quota quota) {
        this.quota = quota;
    }

    @Override
    public void check(ChatCompletionRequest request, ModelRoute route, InvocationContext context) {
        if (!quota.isEnabled()) {
            return;
        }
        String key = "%s:%s:%s".formatted(
                context.tenantId() == null ? "anonymous" : context.tenantId(),
                context.projectId() == null ? "default" : context.projectId(),
                route.id());
        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            long currentMinute = Instant.now().getEpochSecond() / 60;
            if (existing == null || existing.minute != currentMinute) {
                return new WindowCounter(currentMinute, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        if (counter.count.get() > quota.getRequestsPerMinute()) {
            throw new GatewayException(GatewayErrorCode.QUOTA_EXCEEDED, "Tenant quota exceeded", 429);
        }
    }

    private record WindowCounter(long minute, AtomicInteger count) {
    }
}
