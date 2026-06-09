package com.example.modelgateway.core.quota;

import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.core.model.InvocationContext;
import com.example.modelgateway.core.model.ModelRoute;

public interface QuotaService {
    void check(ChatCompletionRequest request, ModelRoute route, InvocationContext context);
}
