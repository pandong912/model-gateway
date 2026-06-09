package com.example.modelgateway.core.usage;

import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.core.model.InvocationContext;
import com.example.modelgateway.core.model.ModelRoute;

public class NoopUsageRecorder implements UsageRecorder {
    @Override
    public void recordSuccess(ChatCompletionRequest request, ChatCompletionResponse response, ModelRoute route, InvocationContext context) {
    }

    @Override
    public void recordFailure(ChatCompletionRequest request, ModelRoute route, InvocationContext context, Throwable error) {
    }
}
