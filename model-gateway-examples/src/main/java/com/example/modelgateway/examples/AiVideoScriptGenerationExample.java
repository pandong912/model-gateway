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

public class AiVideoScriptGenerationExample extends PromptTemplateSupport {
    public AiVideoScriptGenerationExample(ModelGatewayClient modelGatewayClient) {
        super(modelGatewayClient);
    }

    public Mono<ChatCompletionResponse> generateScript(String tenantId, String projectId, String topic) {
        return promptTemplate("script.system", "v1")
                .flatMap(template -> modelGatewayClient.chat(request(tenantId, projectId, topic, template)));
    }

    private ChatCompletionRequest request(String tenantId, String projectId, String topic, PromptTemplate template) {
        return new ChatCompletionRequest(
                tenantId,
                projectId,
                "SCRIPT_GENERATION",
                ModelCapability.TEXT_GENERATION,
                null,
                List.of(ChatMessage.user("请为下面主题生成 30 秒短视频脚本：%s".formatted(topic))),
                Map.of("temperature", 0.7),
                false,
                Map.of(
                        "solution", "ai-video-generation",
                        "promptKey", template.key(),
                        "promptVersion", template.version(),
                        "promptVariables", Map.of("language", "zh-CN")));
    }
}
