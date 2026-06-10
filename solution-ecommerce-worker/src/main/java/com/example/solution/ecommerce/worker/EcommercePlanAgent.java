package com.example.solution.ecommerce.worker;

import com.example.solution.ecommerce.api.EcommerceAssetPlan;
import com.example.solution.ecommerce.api.EcommerceAssetRequest;

public interface EcommercePlanAgent {
    EcommerceAssetPlan generatePlan(EcommerceAssetRequest request);

    EcommerceAssetPlan validateAndRepair(EcommerceAssetPlan plan);
}
