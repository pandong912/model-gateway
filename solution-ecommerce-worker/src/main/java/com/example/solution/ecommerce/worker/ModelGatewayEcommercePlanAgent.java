package com.example.solution.ecommerce.worker;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.example.modelgateway.api.enums.MessageRole;
import com.example.modelgateway.api.enums.ModelCapability;
import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.api.model.ChatMessage;
import com.example.modelgateway.api.model.MediaContent;
import com.example.modelgateway.client.ModelGatewayClient;
import com.example.modelgateway.core.model.PromptTemplate;
import com.example.solution.ecommerce.api.AssetInput;
import com.example.solution.ecommerce.api.DisplayImagePlan;
import com.example.solution.ecommerce.api.DisplayVideoPlan;
import com.example.solution.ecommerce.api.EcommerceAssetPlan;
import com.example.solution.ecommerce.api.EcommerceAssetRequest;
import com.example.solution.ecommerce.api.ModelInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Agent(
        description = "Generate, parse, validate and repair ecommerce asset generation plans from product information and reference images",
        provider = "solution-ecommerce",
        version = "1.0.0"
)
@RequiredArgsConstructor
public class ModelGatewayEcommercePlanAgent implements EcommercePlanAgent {
    private static final String PROMPT_KEY = "ecommerce-asset-design.user";
    private static final String PROMPT_VERSION = "v1";
    private static final String ROUTE_ID = "gemini-3.1-pro-preview";
    private static final String SCENARIO = "ECOMMERCE_ASSET_DESIGN";

    private final ModelGatewayClient modelGatewayClient;
    private final ObjectMapper objectMapper;

    @Override
    public EcommerceAssetPlan generatePlan(EcommerceAssetRequest request) {
        PromptContext promptContext = loadPrompt(request);
        RawAssetPlan rawPlan = callGemini(promptContext);
        ParsedAssetPlan parsedPlan = parseRawPlan(rawPlan);
        return completePlan(parsedPlan, request);
    }

    @Override
    public EcommerceAssetPlan validateAndRepair(EcommerceAssetPlan plan) {
        return repairPlan(plan, null);
    }

    @Action(description = "Load ecommerce asset prompt template and render product/image variables")
    public PromptContext loadPrompt(EcommerceAssetRequest request) {
        PromptTemplate template = promptTemplate().block();
        if (template == null) {
            throw new IllegalStateException("Prompt template not found: " + PROMPT_KEY + ":" + PROMPT_VERSION);
        }
        return new PromptContext(request, template, renderPrompt(request, template), media(request));
    }

    @Action(description = "Call Gemini through model-gateway to generate raw ecommerce asset plan JSON")
    public RawAssetPlan callGemini(PromptContext promptContext) {
        ChatCompletionResponse response = modelGatewayClient.chat(chatRequest(promptContext)).block();
        if (response == null || response.content() == null || response.content().isBlank()) {
            throw new IllegalStateException("Gemini returned an empty ecommerce asset plan");
        }
        return new RawAssetPlan(response.content(), response.traceId(), response.provider(), response.model());
    }

    @Action(description = "Parse Gemini JSON into a typed ecommerce asset plan")
    public ParsedAssetPlan parseRawPlan(RawAssetPlan rawPlan) {
        return new ParsedAssetPlan(parsePlan(rawPlan.content()), rawPlan);
    }

    @AchievesGoal(description = "Produce a valid ecommerce asset generation plan")
    @Action(description = "Validate and repair the parsed ecommerce asset plan")
    public EcommerceAssetPlan completePlan(ParsedAssetPlan parsedPlan, EcommerceAssetRequest request) {
        return repairPlan(parsedPlan.plan(), request);
    }

    private Mono<PromptTemplate> promptTemplate() {
        return modelGatewayClient.prompts()
                .flatMapMany(reactor.core.publisher.Flux::fromIterable)
                .filter(template -> PROMPT_KEY.equals(template.key()))
                .filter(template -> PROMPT_VERSION.equals(template.version()))
                .next();
    }

    private String renderPrompt(EcommerceAssetRequest request, PromptTemplate template) {
        return render(template.content(), Map.of(
                "Product_Info", request.productInfo(),
                "Product_Images", names(request.productImages()),
                "Model_Images", names(request.modelImages())));
    }

    private List<MediaContent> media(EcommerceAssetRequest request) {
        List<MediaContent> media = new ArrayList<>();
        media.addAll(toMedia(request.productImages()));
        media.addAll(toMedia(request.modelImages()));
        return List.copyOf(media);
    }

    private ChatCompletionRequest chatRequest(PromptContext promptContext) {
        EcommerceAssetRequest request = promptContext.request();

        return new ChatCompletionRequest(
                request.tenantId(),
                request.projectId(),
                SCENARIO,
                ModelCapability.TEXT_GENERATION,
                ROUTE_ID,
                List.of(new ChatMessage(
                        MessageRole.USER,
                        promptContext.userPrompt(),
                        null,
                        promptContext.media(),
                        Map.of("imageList", promptContext.media().stream()
                                .map(item -> item.metadata().get("name"))
                                .toList()))),
                Map.of(
                        "temperature", 0.6,
                        "maxOutputTokens", 32768,
                        "responseMimeType", "application/json"),
                false,
                Map.of("solution", "ecommerce-asset-design", "agent", "embabel-compatible"));
    }

    private String render(String template, Map<String, String> variables) {
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private String names(List<AssetInput> assets) {
        if (assets == null || assets.isEmpty()) {
            return "无";
        }
        return assets.stream().map(AssetInput::name).collect(Collectors.joining(", "));
    }

    private List<MediaContent> toMedia(List<AssetInput> assets) {
        if (assets == null || assets.isEmpty()) {
            return List.of();
        }
        return assets.stream()
                .map(asset -> new MediaContent(
                        "image",
                        asset.url(),
                        asset.base64(),
                        asset.mimeType(),
                        Map.of(
                                "name", asset.name(),
                                "sourceFileName", String.valueOf(asset.metadata() == null ? "" : asset.metadata().getOrDefault("sourceFileName", "")))))
                .toList();
    }

    private EcommerceAssetPlan parsePlan(String content) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(content));
            String visualDna = text(root, "visual_dna", "visualDna");
            ModelInfo modelInfo = modelInfo(root.path("model_info").isMissingNode() ? root.path("modelInfo") : root.path("model_info"));
            List<DisplayImagePlan> images = displayImages(root.path("display_images").isMissingNode()
                    ? root.path("displayImages")
                    : root.path("display_images"));
            DisplayVideoPlan video = displayVideo(root.path("display_video").isMissingNode()
                    ? root.path("displayVideo")
                    : root.path("display_video"));
            return new EcommerceAssetPlan(visualDna, modelInfo, images, video, Map.of("source", "gemini-plan"));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse ecommerce asset plan JSON", ex);
        }
    }

    private String stripCodeFence(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return text;
    }

    private ModelInfo modelInfo(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || node.isEmpty()) {
            return ModelInfo.empty();
        }
        return new ModelInfo(
                booleanValue(node, "required", "is_required"),
                booleanValue(node, "userProvided", "is_user_provided"),
                text(node, "image_name", "imageName"),
                text(node, "prompt"),
                text(node, "model_description", "description"),
                stringList(node.path("reference_images").isMissingNode() ? node.path("referenceImages") : node.path("reference_images")));
    }

    private List<DisplayImagePlan> displayImages(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<DisplayImagePlan> images = new ArrayList<>();
        for (JsonNode item : node) {
            images.add(new DisplayImagePlan(
                    item.path("id").asInt(),
                    text(item, "image_name", "imageName"),
                    text(item, "content"),
                    text(item, "prompt"),
                    text(item, "aspect_ratio", "aspectRatio"),
                    stringList(item.path("reference_images").isMissingNode()
                            ? item.path("referenceImages")
                            : item.path("reference_images"))));
        }
        return images;
    }

    private DisplayVideoPlan displayVideo(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return new DisplayVideoPlan("display_video.mp4", "", "3:4", 10, List.of());
        }
        return new DisplayVideoPlan(
                text(node, "video_name", "videoName"),
                text(node, "video_prompt", "prompt"),
                text(node, "aspect_ratio", "aspectRatio"),
                node.path("duration_seconds").asInt(node.path("durationSeconds").asInt(10)),
                stringList(node.path("reference_images").isMissingNode() ? node.path("referenceImages") : node.path("reference_images")));
    }

    private void validate(EcommerceAssetPlan plan) {
        if (plan.displayImages() == null || plan.displayImages().size() != 10) {
            throw new IllegalArgumentException("Ecommerce plan must contain exactly 10 display image plans");
        }
        if (plan.displayVideo() == null) {
            throw new IllegalArgumentException("Ecommerce plan must contain display video plan");
        }
    }

    private EcommerceAssetPlan repairPlan(EcommerceAssetPlan plan, EcommerceAssetRequest request) {
        List<DisplayImagePlan> repairedImages = repairDisplayImages(plan.displayImages(), request);
        DisplayVideoPlan repairedVideo = repairDisplayVideo(plan.displayVideo(), repairedImages);
        ModelInfo repairedModelInfo = plan.modelInfo() == null ? inferModelInfo(request) : plan.modelInfo();
        EcommerceAssetPlan repairedPlan = new EcommerceAssetPlan(
                emptyToDefault(plan.visualDna(), "clean ecommerce product visual style"),
                repairedModelInfo,
                repairedImages,
                repairedVideo,
                mergeMetadata(plan.metadata(), Map.of("repaired", true)));
        validate(repairedPlan);
        return repairedPlan;
    }

    private List<DisplayImagePlan> repairDisplayImages(List<DisplayImagePlan> images, EcommerceAssetRequest request) {
        ArrayList<DisplayImagePlan> repaired = new ArrayList<>();
        if (images != null) {
            for (DisplayImagePlan image : images) {
                if (repaired.size() >= 10) {
                    break;
                }
                int fallbackId = repaired.size() + 1;
                repaired.add(new DisplayImagePlan(
                        image.id() <= 0 ? fallbackId : image.id(),
                        emptyToDefault(image.imageName(), imageName(fallbackId)),
                        emptyToDefault(image.content(), "ecommerce display image variant " + fallbackId),
                        emptyToDefault(image.prompt(), fallbackImagePrompt(fallbackId, request)),
                        emptyToDefault(image.aspectRatio(), "3:4"),
                        image.referenceImages() == null ? defaultReferences(request) : image.referenceImages()));
            }
        }
        while (repaired.size() < 10) {
            int id = repaired.size() + 1;
            repaired.add(new DisplayImagePlan(
                    id,
                    imageName(id),
                    "ecommerce display image variant " + id,
                    fallbackImagePrompt(id, request),
                    "3:4",
                    defaultReferences(request)));
        }
        return List.copyOf(repaired);
    }

    private DisplayVideoPlan repairDisplayVideo(DisplayVideoPlan video, List<DisplayImagePlan> images) {
        String firstFrameName = images.get(Math.min(6, images.size() - 1)).imageName();
        if (video == null) {
            return new DisplayVideoPlan(
                    "display_video.mp4",
                    "Generate a short ecommerce product video from the selected first frame, highlighting product texture, shine and usage scenario.",
                    "3:4",
                    10,
                    List.of(firstFrameName));
        }
        return new DisplayVideoPlan(
                emptyToDefault(video.videoName(), "display_video.mp4"),
                emptyToDefault(video.prompt(), "Generate a short ecommerce product video with smooth camera motion and premium commercial lighting."),
                emptyToDefault(video.aspectRatio(), "3:4"),
                video.durationSeconds() <= 0 ? 10 : video.durationSeconds(),
                video.referenceImages() == null || video.referenceImages().isEmpty() ? List.of(firstFrameName) : video.referenceImages());
    }

    private ModelInfo inferModelInfo(EcommerceAssetRequest request) {
        if (request == null) {
            return ModelInfo.empty();
        }
        boolean userProvided = request.modelImages() != null && !request.modelImages().isEmpty();
        return new ModelInfo(
                !userProvided,
                userProvided,
                userProvided ? request.modelImages().getFirst().name() : "generated_model_1",
                userProvided ? "" : "Generate a suitable ecommerce model reference image for the product.",
                userProvided ? "User provided model reference image" : "Generated model reference image",
                defaultReferences(request));
    }

    private String fallbackImagePrompt(int id, EcommerceAssetRequest request) {
        return """
                Create ecommerce display image variant %d for this product:
                %s
                Use premium commercial lighting, clear product visibility, and conversion-oriented composition.
                """.formatted(id, request == null ? "" : request.productInfo()).trim();
    }

    private List<String> defaultReferences(EcommerceAssetRequest request) {
        if (request == null) {
            return List.of();
        }
        ArrayList<String> references = new ArrayList<>();
        if (request.productImages() != null) {
            request.productImages().stream().map(AssetInput::name).forEach(references::add);
        }
        if (request.modelImages() != null) {
            request.modelImages().stream().map(AssetInput::name).forEach(references::add);
        }
        return List.copyOf(references);
    }

    private String imageName(int id) {
        return "display_%02d.png".formatted(id);
    }

    private String emptyToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private Map<String, Object> mergeMetadata(Map<String, Object> metadata, Map<String, Object> overrides) {
        java.util.HashMap<String, Object> merged = new java.util.HashMap<>();
        if (metadata != null) {
            merged.putAll(metadata);
        }
        merged.putAll(overrides);
        return Map.copyOf(merged);
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (!value.isMissingNode() && !value.isNull()) {
                return value.asText("");
            }
        }
        return "";
    }

    private boolean booleanValue(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (!value.isMissingNode() && !value.isNull()) {
                return value.asBoolean(false);
            }
        }
        return false;
    }

    private List<String> stringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return values;
    }

    public record PromptContext(
            EcommerceAssetRequest request,
            PromptTemplate template,
            String userPrompt,
            List<MediaContent> media
    ) {
    }

    public record RawAssetPlan(
            String content,
            String traceId,
            String provider,
            String model
    ) {
    }

    public record ParsedAssetPlan(
            EcommerceAssetPlan plan,
            RawAssetPlan rawPlan
    ) {
    }
}
