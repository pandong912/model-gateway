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

public class AiVideoShotPromptRewriteExample extends PromptTemplateSupport {
    public AiVideoShotPromptRewriteExample(ModelGatewayClient modelGatewayClient) {
        super(modelGatewayClient);
    }

    public Mono<ChatCompletionResponse> rewriteShotPrompt(String tenantId, String projectId, String roughPrompt) {
        return promptTemplate("shot-prompt-rewrite.system", "v1")
                .flatMap(template -> modelGatewayClient.chat(request(tenantId, projectId, roughPrompt, template)));
    }

    private ChatCompletionRequest request(String tenantId, String projectId, String roughPrompt, PromptTemplate template) {
        return new ChatCompletionRequest(
                tenantId,
                projectId,
                "SHOT_PROMPT_REWRITE",
                ModelCapability.TEXT_GENERATION,
                null,
                List.of(ChatMessage.user("请改写下面镜头提示词，使其更适合视频生成模型：%s".formatted(roughPrompt))),
                Map.of("temperature", 0.5),
                false,
                Map.of(
                        "solution", "ai-video-generation",
                        "promptKey", template.key(),
                        "promptVersion", template.version(),
                        "promptVariables", Map.of("language", "zh-CN")));
    }
}
