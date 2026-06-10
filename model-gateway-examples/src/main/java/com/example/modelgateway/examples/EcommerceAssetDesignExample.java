package com.example.modelgateway.examples;

import com.example.modelgateway.api.enums.ModelCapability;
import com.example.modelgateway.api.enums.MessageRole;
import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.api.model.ChatMessage;
import com.example.modelgateway.api.model.MediaContent;
import com.example.modelgateway.client.ModelGatewayClient;
import com.example.modelgateway.core.model.PromptTemplate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import reactor.core.publisher.Mono;

public class EcommerceAssetDesignExample extends PromptTemplateSupport {
    private static final String PROMPT_KEY = "ecommerce-asset-design.user";
    private static final String PROMPT_VERSION = "v1";
    private static final String GEMINI_31_PRO_PREVIEW_ROUTE = "gemini-3.1-pro-preview";

    public EcommerceAssetDesignExample(ModelGatewayClient modelGatewayClient) {
        super(modelGatewayClient);
    }

    public Mono<ChatCompletionResponse> generateAssetPlan(
            String tenantId,
            String projectId,
            UserPromptInput input
    ) {
        return promptTemplate(PROMPT_KEY, PROMPT_VERSION)
                .flatMap(template -> {
                    try {
                        return modelGatewayClient.chat(request(tenantId, projectId, input, template));
                    } catch (IOException ex) {
                        return Mono.error(ex);
                    }
                });
    }

    private ChatCompletionRequest request(
            String tenantId,
            String projectId,
            UserPromptInput input,
            PromptTemplate template
    ) throws IOException {
        String userPrompt = render(template, variables(input));
        List<MediaContent> imageList = imageList(input);
        return new ChatCompletionRequest(
                tenantId,
                projectId,
                "ECOMMERCE_ASSET_DESIGN",
                ModelCapability.TEXT_GENERATION,
                GEMINI_31_PRO_PREVIEW_ROUTE,
                List.of(new ChatMessage(
                        MessageRole.USER,
                        userPrompt,
                        null,
                        imageList,
                        Map.of("imageList", imageList.stream()
                                .map(media -> media.metadata().get("name"))
                                .toList()))),
                Map.of(
                        "temperature", 0.6,
                        "maxOutputTokens", 32768,
                        "responseMimeType", "application/json"),
                false,
                Map.of(
                        "solution", "ecommerce-asset-design",
                        "clientRenderedPromptKey", template.key(),
                        "clientRenderedPromptVersion", template.version()));
    }

    private Map<String, String> variables(UserPromptInput input) {
        return Map.of(
                "Product_Info", input.productInfo(),
                "Product_Images", formatImageNames("product", input.productImageFiles()),
                "Model_Images", formatImageNames("model", input.modelImageFiles()));
    }

    private String formatImageNames(String prefix, List<Path> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return "无";
        }
        return IntStream.range(0, imageFiles.size())
                .mapToObj(index -> prefix + "_" + (index + 1))
                .collect(Collectors.joining(", "));
    }

    private List<MediaContent> imageList(UserPromptInput input) throws IOException {
        List<MediaContent> productImages = mediaList("product", input.productImageFiles());
        List<MediaContent> modelImages = mediaList("model", input.modelImageFiles());
        return Stream.concat(productImages.stream(), modelImages.stream()).toList();
    }

    private List<MediaContent> mediaList(String prefix, List<Path> imageFiles) throws IOException {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return List.of();
        }
        ArrayList<MediaContent> media = new ArrayList<>();
        for (int index = 0; index < imageFiles.size(); index++) {
            Path imageFile = imageFiles.get(index);
            String name = prefix + "_" + (index + 1);
            media.add(new MediaContent(
                    "image",
                    null,
                    Base64.getEncoder().encodeToString(Files.readAllBytes(imageFile)),
                    mimeType(imageFile),
                    Map.of(
                            "name", name,
                            "sourceFileName", imageFile.getFileName().toString())));
        }
        return List.copyOf(media);
    }

    private String mimeType(Path imageFile) throws IOException {
        String detected = Files.probeContentType(imageFile);
        return detected == null || detected.isBlank() ? "image/jpeg" : detected;
    }

    public record UserPromptInput(
            String productInfo,
            List<Path> productImageFiles,
            List<Path> modelImageFiles
    ) {
    }
}
