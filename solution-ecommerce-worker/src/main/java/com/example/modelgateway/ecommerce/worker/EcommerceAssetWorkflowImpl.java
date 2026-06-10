package com.example.modelgateway.ecommerce.worker;

import com.example.modelgateway.ecommerce.api.DisplayImagePlan;
import com.example.modelgateway.ecommerce.api.EcommerceAssetJobStatus;
import com.example.modelgateway.ecommerce.api.EcommerceAssetPlan;
import com.example.modelgateway.ecommerce.api.EcommerceAssetRequest;
import com.example.modelgateway.ecommerce.api.EcommerceAssetResult;
import com.example.modelgateway.ecommerce.api.ImageResult;
import com.example.modelgateway.ecommerce.api.ModelInfo;
import com.example.modelgateway.ecommerce.api.VideoResult;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcommerceAssetWorkflowImpl implements EcommerceAssetWorkflow {
    private final EcommerceAssetActivities activities = Workflow.newActivityStub(
            EcommerceAssetActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(2.0)
                            .setMaximumInterval(Duration.ofMinutes(2))
                            .setMaximumAttempts(3)
                            .build())
                    .build());

    private EcommerceAssetJobStatus status = EcommerceAssetJobStatus.PLANNING;

    @Override
    public EcommerceAssetResult run(EcommerceAssetRequest request) {
        String jobId = Workflow.getInfo().getWorkflowId();

        status = EcommerceAssetJobStatus.PLANNING;
        EcommerceAssetPlan plan = activities.validateAndRepairPlan(activities.generatePlan(request));

        Map<String, ImageResult> generatedReferences = new HashMap<>();
        ImageResult modelImage = null;
        ModelInfo modelInfo = plan.modelInfo() == null ? ModelInfo.empty() : plan.modelInfo();
        if (modelInfo.required() && !modelInfo.userProvided()) {
            status = EcommerceAssetJobStatus.MODEL_IMAGE_GENERATING;
            modelImage = activities.generateModelImage(modelInfo, request);
            generatedReferences.put(modelImage.imageName(), modelImage);
        }

        status = EcommerceAssetJobStatus.DISPLAY_IMAGES_GENERATING;
        List<Promise<ImageResult>> imagePromises = plan.displayImages().stream()
                .map(imagePlan -> Async.function(activities::generateDisplayImage, imagePlan, generatedReferences))
                .toList();
        List<ImageResult> displayImages = imagePromises.stream()
                .map(Promise::get)
                .toList();
        displayImages.forEach(image -> generatedReferences.put(image.imageName(), image));

        status = EcommerceAssetJobStatus.VIDEO_GENERATING;
        ImageResult firstFrame = firstFrame(plan, displayImages);
        VideoResult video = activities.generateVideo(plan.displayVideo(), firstFrame);

        status = EcommerceAssetJobStatus.COMPLETED;
        return activities.finalizeResult(jobId, plan, modelImage, displayImages, video);
    }

    @Override
    public EcommerceAssetJobStatus status() {
        return status;
    }

    private ImageResult firstFrame(EcommerceAssetPlan plan, List<ImageResult> displayImages) {
        if (plan.displayVideo() != null && plan.displayVideo().referenceImages() != null) {
            for (String reference : plan.displayVideo().referenceImages()) {
                for (ImageResult image : displayImages) {
                    if (image.imageName().equals(reference)) {
                        return image;
                    }
                }
            }
        }
        return displayImages.stream()
                .filter(image -> image.imageName() != null && image.imageName().contains("_07"))
                .findFirst()
                .orElse(displayImages.get(Math.min(6, displayImages.size() - 1)));
    }
}
