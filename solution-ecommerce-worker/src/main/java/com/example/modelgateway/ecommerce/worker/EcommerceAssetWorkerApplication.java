package com.example.modelgateway.ecommerce.worker;

import com.example.modelgateway.client.ModelGatewayClient;
import com.example.modelgateway.client.ModelGatewayClientProperties;
import com.example.modelgateway.client.WebClientModelGatewayClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.net.URI;
import org.springframework.web.reactive.function.client.WebClient;

public class EcommerceAssetWorkerApplication {
    public static final String TASK_QUEUE = "ecommerce-asset-task-queue";

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient workflowClient = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(EcommerceAssetWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities());
        factory.start();
        System.out.println("Ecommerce asset worker started. taskQueue=" + TASK_QUEUE);
    }

    static EcommerceAssetActivities activities() {
        return new EcommerceAssetActivitiesImpl(
                new ModelGatewayEcommercePlanAgent(modelGatewayClient(), new ObjectMapper()),
                new MockKlingClient());
    }

    static ModelGatewayClient modelGatewayClient() {
        ModelGatewayClientProperties properties = new ModelGatewayClientProperties();
        properties.setBaseUrl(URI.create(env("MODEL_GATEWAY_BASE_URL", "http://localhost:9050")));
        String apiKey = env("MODEL_GATEWAY_API_KEY", "");
        if (!apiKey.isBlank()) {
            properties.setApiKey(apiKey);
        }
        return new WebClientModelGatewayClient(WebClient.builder(), properties);
    }

    static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
