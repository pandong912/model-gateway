package com.example.modelgateway.ecommerce.worker;

import com.example.modelgateway.ecommerce.api.DisplayImagePlan;
import com.example.modelgateway.ecommerce.api.DisplayVideoPlan;
import com.example.modelgateway.ecommerce.api.EcommerceAssetRequest;
import com.example.modelgateway.ecommerce.api.ImageResult;
import com.example.modelgateway.ecommerce.api.ModelInfo;
import com.example.modelgateway.ecommerce.api.VideoResult;
import java.util.Map;

public interface KlingClient {
    ImageResult generateModelImage(ModelInfo modelInfo, EcommerceAssetRequest request);

    ImageResult generateDisplayImage(DisplayImagePlan imagePlan, Map<String, ImageResult> generatedReferences);

    VideoResult generateVideo(DisplayVideoPlan videoPlan, ImageResult firstFrame);
}
