package com.example.solution.ecommerce.worker;

import com.example.solution.ecommerce.api.DisplayImagePlan;
import com.example.solution.ecommerce.api.DisplayVideoPlan;
import com.example.solution.ecommerce.api.EcommerceAssetRequest;
import com.example.solution.ecommerce.api.ImageResult;
import com.example.solution.ecommerce.api.ModelInfo;
import com.example.solution.ecommerce.api.VideoResult;
import java.util.List;
import java.util.Map;

public class MockKlingClient implements KlingClient {
    @Override
    public ImageResult generateModelImage(ModelInfo modelInfo, EcommerceAssetRequest request) {
        String imageName = modelInfo.imageName() == null || modelInfo.imageName().isBlank()
                ? "generated_model.png"
                : modelInfo.imageName();
        return new ImageResult(
                imageName,
                "mock://kling/images/" + imageName,
                "mock-model-job-" + sanitize(imageName),
                modelInfo.referenceImages() == null ? List.of() : modelInfo.referenceImages(),
                Map.of("mock", true, "kind", "model-image", "prompt", nullToEmpty(modelInfo.prompt())));
    }

    @Override
    public ImageResult generateDisplayImage(DisplayImagePlan imagePlan, Map<String, ImageResult> generatedReferences) {
        return new ImageResult(
                imagePlan.imageName(),
                "mock://kling/images/" + imagePlan.imageName(),
                "mock-image-job-" + imagePlan.id(),
                imagePlan.referenceImages() == null ? List.of() : imagePlan.referenceImages(),
                Map.of(
                        "mock", true,
                        "kind", "display-image",
                        "prompt", nullToEmpty(imagePlan.prompt()),
                        "generatedReferenceCount", generatedReferences == null ? 0 : generatedReferences.size()));
    }

    @Override
    public VideoResult generateVideo(DisplayVideoPlan videoPlan, ImageResult firstFrame) {
        return new VideoResult(
                videoPlan.videoName(),
                "mock://kling/videos/" + videoPlan.videoName(),
                "mock-video-job-" + sanitize(videoPlan.videoName()),
                videoPlan.referenceImages() == null ? List.of() : videoPlan.referenceImages(),
                Map.of(
                        "mock", true,
                        "kind", "display-video",
                        "prompt", nullToEmpty(videoPlan.prompt()),
                        "firstFrame", firstFrame == null ? "" : firstFrame.imageName()));
    }

    private String sanitize(String value) {
        return value == null ? "unknown" : value.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
