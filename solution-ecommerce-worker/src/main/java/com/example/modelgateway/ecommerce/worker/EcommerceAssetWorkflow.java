package com.example.modelgateway.ecommerce.worker;

import com.example.modelgateway.ecommerce.api.EcommerceAssetRequest;
import com.example.modelgateway.ecommerce.api.EcommerceAssetResult;
import com.example.modelgateway.ecommerce.api.EcommerceAssetJobStatus;
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
