package com.example.modelgateway.ecommerce.worker;

import com.example.modelgateway.ecommerce.api.EcommerceAssetPlan;
import com.example.modelgateway.ecommerce.api.EcommerceAssetRequest;

public interface EcommercePlanAgent {
    EcommerceAssetPlan generatePlan(EcommerceAssetRequest request);

    EcommerceAssetPlan validateAndRepair(EcommerceAssetPlan plan);
}
