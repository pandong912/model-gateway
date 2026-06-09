package com.example.modelgateway.examples;

import com.example.modelgateway.api.enums.ModelCapability;
import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.api.model.ChatMessage;
import com.example.modelgateway.api.model.MediaContent;
import com.example.modelgateway.client.ModelGatewayClient;
import com.example.modelgateway.core.model.PromptTemplate;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;

public class AiVideoAssetTaggingExample extends PromptTemplateSupport {
    public AiVideoAssetTaggingExample(ModelGatewayClient modelGatewayClient) {
        super(modelGatewayClient);
    }

    public Mono<ChatCompletionResponse> analyzeVideoAsset(
            String tenantId,
            String projectId,
            String geminiFileUri
    ) {
        return promptTemplate("asset-tagging.system", "v1")
                .flatMap(template -> modelGatewayClient.chat(request(tenantId, projectId, geminiFileUri, template)));
    }

    private ChatCompletionRequest request(String tenantId, String projectId, String geminiFileUri, PromptTemplate template) {
        MediaContent video = new MediaContent(
                "video",
                null,
                null,
                "video/mp4",
                Map.of("geminiFileUri", geminiFileUri));

        return new ChatCompletionRequest(
                tenantId,
                projectId,
                "ASSET_TAGGING",
                ModelCapability.VISION_UNDERSTANDING,
                "gemini-vision",
                List.of(new ChatMessage(
                        com.example.modelgateway.api.enums.MessageRole.USER,
                        "请分析这个视频素材，输出主体、场景、动作、镜头运动、风格标签和可用于生成模型的提示词。",
                        null,
                        List.of(video),
                        Map.of())),
                Map.of("temperature", 0.2),
                false,
                Map.of(
                        "solution", "ai-video-generation",
                        "promptKey", template.key(),
                        "promptVersion", template.version()));
    }
}
