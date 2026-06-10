package com.example.modelgateway.ecommerce.worker;

import com.example.modelgateway.ecommerce.api.DisplayImagePlan;
import com.example.modelgateway.ecommerce.api.DisplayVideoPlan;
import com.example.modelgateway.ecommerce.api.EcommerceAssetJobStatus;
import com.example.modelgateway.ecommerce.api.EcommerceAssetPlan;
import com.example.modelgateway.ecommerce.api.EcommerceAssetRequest;
import com.example.modelgateway.ecommerce.api.EcommerceAssetResult;
import com.example.modelgateway.ecommerce.api.ImageResult;
import com.example.modelgateway.ecommerce.api.ModelInfo;
import com.example.modelgateway.ecommerce.api.VideoResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EcommerceAssetActivitiesImpl implements EcommerceAssetActivities {
    private final EcommercePlanAgent planAgent;
    private final KlingClient klingClient;

    @Override
    public EcommerceAssetPlan generatePlan(EcommerceAssetRequest request) {
        return planAgent.generatePlan(request);
    }

    @Override
    public EcommerceAssetPlan validateAndRepairPlan(EcommerceAssetPlan plan) {
        return planAgent.validateAndRepair(plan);
    }

    @Override
    public ImageResult generateModelImage(ModelInfo modelInfo, EcommerceAssetRequest request) {
        return klingClient.generateModelImage(modelInfo, request);
    }

    @Override
    public ImageResult generateDisplayImage(DisplayImagePlan imagePlan, Map<String, ImageResult> generatedReferences) {
        return klingClient.generateDisplayImage(imagePlan, generatedReferences);
    }

    @Override
    public VideoResult generateVideo(DisplayVideoPlan videoPlan, ImageResult firstFrame) {
        return klingClient.generateVideo(videoPlan, firstFrame);
    }

    @Override
    public EcommerceAssetResult finalizeResult(
            String jobId,
            EcommerceAssetPlan plan,
            ImageResult modelImage,
            List<ImageResult> displayImages,
            VideoResult videoResult
    ) {
        return new EcommerceAssetResult(
                jobId,
                EcommerceAssetJobStatus.COMPLETED,
                plan,
                modelImage,
                displayImages,
                videoResult,
                Map.of("mockKling", true));
    }
}
