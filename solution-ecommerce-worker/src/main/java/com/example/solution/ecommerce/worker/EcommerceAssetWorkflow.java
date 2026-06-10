package com.example.solution.ecommerce.worker;

import com.example.solution.ecommerce.api.EcommerceAssetRequest;
import com.example.solution.ecommerce.api.EcommerceAssetResult;
import com.example.solution.ecommerce.api.EcommerceAssetJobStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface EcommerceAssetWorkflow {
    @WorkflowMethod
    EcommerceAssetResult run(EcommerceAssetRequest request);

    @QueryMethod
    EcommerceAssetJobStatus status();
}
