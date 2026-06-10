package com.example.solution.ecommerce.worker;

import com.example.solution.ecommerce.api.DisplayImagePlan;
import com.example.solution.ecommerce.api.DisplayVideoPlan;
import com.example.solution.ecommerce.api.EcommerceAssetRequest;
import com.example.solution.ecommerce.api.ImageResult;
import com.example.solution.ecommerce.api.ModelInfo;
import com.example.solution.ecommerce.api.VideoResult;
import java.util.Map;

public interface KlingClient {
    ImageResult generateModelImage(ModelInfo modelInfo, EcommerceAssetRequest request);

    ImageResult generateDisplayImage(DisplayImagePlan imagePlan, Map<String, ImageResult> generatedReferences);

    VideoResult generateVideo(DisplayVideoPlan videoPlan, ImageResult firstFrame);
}
