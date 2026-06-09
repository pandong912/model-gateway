package com.example.modelgateway.examples;

import com.example.modelgateway.api.enums.ModelCapability;
import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.api.model.ChatMessage;
import com.example.modelgateway.client.ModelGatewayClient;
import com.example.modelgateway.core.model.PromptTemplate;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

public class AiVideoStoryboardExample extends PromptTemplateSupport {
    public AiVideoStoryboardExample(ModelGatewayClient modelGatewayClient) {
        super(modelGatewayClient);
    }

    public Mono<ChatCompletionResponse> generateStoryboard(String tenantId, String projectId, String storyIdea) {
        return promptTemplate("storyboard.system", "v1")
                .flatMap(template -> modelGatewayClient.chat(request(tenantId, projectId, storyIdea, template)));
    }

    private ChatCompletionRequest request(String tenantId, String projectId, String storyIdea, PromptTemplate template) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                tenantId,
                projectId,
                "STORYBOARD_PLANNING",
                ModelCapability.JSON_MODE,
                null,
                List.of(
                        ChatMessage.user("""
                                请根据下面创意生成 6 个视频镜头分镜，字段包含 shotNo、scene、camera、visualPrompt、durationSeconds。
                                创意：%s
                                """.formatted(storyIdea))
                ),
                Map.of("temperature", 0.4, "response_format", Map.of("type", "json_object")),
                false,
                Map.of(
                        "solution", "ai-video-generation",
                        "promptKey", template.key(),
                        "promptVersion", template.version(),
                        "promptVariables", Map.of("videoType", "短视频", "language", "zh-CN")));
        return request;
    }
}
