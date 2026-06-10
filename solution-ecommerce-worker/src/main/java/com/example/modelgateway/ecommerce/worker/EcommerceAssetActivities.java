package com.example.modelgateway.ecommerce.worker;

import com.example.modelgateway.ecommerce.api.DisplayImagePlan;
import com.example.modelgateway.ecommerce.api.DisplayVideoPlan;
import com.example.modelgateway.ecommerce.api.EcommerceAssetPlan;
import com.example.modelgateway.ecommerce.api.EcommerceAssetRequest;
import com.example.modelgateway.ecommerce.api.EcommerceAssetResult;
import com.example.modelgateway.ecommerce.api.ImageResult;
import com.example.modelgateway.ecommerce.api.ModelInfo;
import com.example.modelgateway.ecommerce.api.VideoResult;
import io.temporal.activity.ActivityInterface;
import java.util.List;
import java.util.Map;

@ActivityInterface
public interface EcommerceAssetActivities {
    EcommerceAssetPlan generatePlan(EcommerceAssetRequest request);

    EcommerceAssetPlan validateAndRepairPlan(EcommerceAssetPlan plan);

    ImageResult generateModelImage(ModelInfo modelInfo, EcommerceAssetRequest request);

    ImageResult generateDisplayImage(DisplayImagePlan imagePlan, Map<String, ImageResult> generatedReferences);

    VideoResult generateVideo(DisplayVideoPlan videoPlan, ImageResult firstFrame);

    EcommerceAssetResult finalizeResult(
            String jobId,
            EcommerceAssetPlan plan,
            ImageResult modelImage,
            List<ImageResult> displayImages,
            VideoResult videoResult
    );
}
