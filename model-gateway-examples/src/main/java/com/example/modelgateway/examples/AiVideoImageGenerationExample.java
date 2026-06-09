package com.example.modelgateway.examples;

import com.example.modelgateway.api.enums.MessageRole;
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

public class AiVideoImageGenerationExample extends PromptTemplateSupport {
    public AiVideoImageGenerationExample(ModelGatewayClient modelGatewayClient) {
        super(modelGatewayClient);
    }

    public Mono<ChatCompletionResponse> generateVisualAsset(
            String tenantId,
            String projectId,
            String visualBrief
    ) {
        return promptTemplate("image-generation.system", "v1")
                .flatMap(template -> modelGatewayClient.chat(imageGenerationRequest(tenantId, projectId, visualBrief, template)));
    }

    public Mono<ChatCompletionResponse> editVisualAsset(
            String tenantId,
            String projectId,
            String editInstruction,
            String referenceImageBase64
    ) {
        return promptTemplate("image-editing.system", "v1")
                .flatMap(template -> modelGatewayClient.chat(imageEditingRequest(
                        tenantId,
                        projectId,
                        editInstruction,
                        referenceImageBase64,
                        template)));
    }

    private ChatCompletionRequest imageGenerationRequest(
            String tenantId,
            String projectId,
            String visualBrief,
            PromptTemplate template
    ) {
        return new ChatCompletionRequest(
                tenantId,
                projectId,
                "IMAGE_GENERATION",
                ModelCapability.IMAGE_GENERATION,
                "gemini-image",
                List.of(ChatMessage.user(visualBrief)),
                Map.of("responseModalities", List.of("TEXT", "IMAGE")),
                false,
                Map.of(
                        "solution", "ai-video-generation",
                        "promptKey", template.key(),
                        "promptVersion", template.version(),
                        "promptVariables", Map.of(
                                "aspectRatio", "16:9",
                                "visualStyle", "cinematic",
                                "language", "zh-CN")));
    }

    private ChatCompletionRequest imageEditingRequest(
            String tenantId,
            String projectId,
            String editInstruction,
            String referenceImageBase64,
            PromptTemplate template
    ) {
        MediaContent referenceImage = new MediaContent(
                "image",
                null,
                referenceImageBase64,
                "image/png",
                Map.of("purpose", "reference"));

        return new ChatCompletionRequest(
                tenantId,
                projectId,
                "IMAGE_EDITING",
                ModelCapability.IMAGE_EDITING,
                "gemini-image",
                List.of(new ChatMessage(
                        MessageRole.USER,
                        editInstruction,
                        null,
                        List.of(referenceImage),
                        Map.of())),
                Map.of("responseModalities", List.of("TEXT", "IMAGE")),
                false,
                Map.of(
                        "solution", "ai-video-generation",
                        "promptKey", template.key(),
                        "promptVersion", template.version(),
                        "promptVariables", Map.of("language", "zh-CN")));
    }
}
