package com.example.modelgateway.core.quota;

import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.core.model.InvocationContext;
import com.example.modelgateway.core.model.ModelRoute;

public class NoopQuotaService implements QuotaService {
    @Override
    public void check(ChatCompletionRequest request, ModelRoute route, InvocationContext context) {
        // Intentionally empty. The service module can replace this with tenant-aware quotas.
    }
}
