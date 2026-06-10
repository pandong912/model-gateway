package com.example.solution.ecommerce.worker;

import com.example.solution.ecommerce.api.DisplayImagePlan;
import com.example.solution.ecommerce.api.DisplayVideoPlan;
import com.example.solution.ecommerce.api.EcommerceAssetPlan;
import com.example.solution.ecommerce.api.EcommerceAssetRequest;
import com.example.solution.ecommerce.api.EcommerceAssetResult;
import com.example.solution.ecommerce.api.ImageResult;
import com.example.solution.ecommerce.api.ModelInfo;
import com.example.solution.ecommerce.api.VideoResult;
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
