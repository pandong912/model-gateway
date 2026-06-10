package com.example.solution.ecommerce.worker;

import com.example.solution.ecommerce.api.AssetInput;
import com.example.solution.ecommerce.api.EcommerceAssetRequest;
import com.example.solution.ecommerce.api.EcommerceAssetResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcommerceAssetWorkflowRunner {
    private static final DateTimeFormatter OUTPUT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) {
        try {
            if (Arrays.asList(args).contains("--help")) {
                System.out.println(usage());
                return;
            }

            Map<String, String> options = parseArgs(args);
            RunConfig config = RunConfig.from(options);
            validate(config);

            WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
            WorkflowClient workflowClient = WorkflowClient.newInstance(service);
            WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
            Worker worker = factory.newWorker(EcommerceAssetWorkerApplication.TASK_QUEUE);
            worker.registerWorkflowImplementationTypes(EcommerceAssetWorkflowImpl.class);
            worker.registerActivitiesImplementations(EcommerceAssetWorkerApplication.activities());
            factory.start();

            EcommerceAssetWorkflow workflow = workflowClient.newWorkflowStub(
                    EcommerceAssetWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(EcommerceAssetWorkerApplication.TASK_QUEUE)
                            .setWorkflowId("ecommerce-asset-" + System.currentTimeMillis())
                            .build());

            EcommerceAssetResult result = workflow.run(request(config));
            writeResult(result, config.outputFile());
            factory.shutdown();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.err.println();
            System.err.println(usage());
            System.exit(1);
        }
    }

    private static EcommerceAssetRequest request(RunConfig config) throws java.io.IOException {
        return new EcommerceAssetRequest(
                config.tenantId(),
                config.projectId(),
                config.productInfo(),
                assetInputs("product", config.productImageFiles()),
                assetInputs("model", config.modelImageFiles()),
                Map.of("source", "local-runner"));
    }

    private static List<AssetInput> assetInputs(String prefix, List<Path> imageFiles) throws java.io.IOException {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<AssetInput> assets = new java.util.ArrayList<>();
        for (int index = 0; index < imageFiles.size(); index++) {
            Path imageFile = imageFiles.get(index);
            String name = prefix + "_" + (index + 1);
            assets.add(new AssetInput(
                    name,
                    null,
                    Base64.getEncoder().encodeToString(Files.readAllBytes(imageFile)),
                    mimeType(imageFile),
                    Map.of("sourceFileName", imageFile.getFileName().toString())));
        }
        return List.copyOf(assets);
    }

    private static String mimeType(Path imageFile) throws java.io.IOException {
        String detected = Files.probeContentType(imageFile);
        return detected == null || detected.isBlank() ? "image/jpeg" : detected;
    }

    private static void validate(RunConfig config) {
        if (config.productInfo() == null || config.productInfo().isBlank()) {
            throw new IllegalArgumentException("Missing product info. Use --product-info or --product-info-file.");
        }
        if (config.productImageFiles().isEmpty()) {
            throw new IllegalArgumentException("Missing product images. Use --product-images with comma-separated file paths.");
        }
        config.productImageFiles().forEach(EcommerceAssetWorkflowRunner::validateFile);
        config.modelImageFiles().forEach(EcommerceAssetWorkflowRunner::validateFile);
    }

    private static void validateFile(Path path) {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("Image file is not readable: " + path);
        }
    }

    private static void writeResult(EcommerceAssetResult result, Path outputFile) throws java.io.IOException {
        if (outputFile == null) {
            System.out.println(OBJECT_MAPPER.writeValueAsString(result));
            return;
        }
        Path resolvedOutputFile = timestampedOutputFile(outputFile);
        Path parent = resolvedOutputFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        OBJECT_MAPPER.writeValue(resolvedOutputFile.toFile(), result);
        System.out.println("Ecommerce asset result written to: " + resolvedOutputFile.toAbsolutePath());
        System.out.println("jobId=" + result.jobId() + ", status=" + result.status());
    }

    private static Path timestampedOutputFile(Path outputFile) {
        String timestamp = LocalDateTime.now().format(OUTPUT_TIMESTAMP);
        if (Files.isDirectory(outputFile)) {
            return outputFile.resolve("ecommerce-asset-result-" + timestamp + ".json");
        }
        Path parent = outputFile.getParent();
        String fileName = outputFile.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String timestampedFileName = extensionIndex > 0
                ? fileName.substring(0, extensionIndex) + "-" + timestamp + fileName.substring(extensionIndex)
                : fileName + "-" + timestamp + ".json";
        return parent == null ? Path.of(timestampedFileName) : parent.resolve(timestampedFileName);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
            String keyValue = arg.substring(2);
            int separator = keyValue.indexOf('=');
            if (separator >= 0) {
                options.put(keyValue.substring(0, separator), keyValue.substring(separator + 1));
            } else {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for argument: " + arg);
                }
                options.put(keyValue, args[++index]);
            }
        }
        return options;
    }

    private static String option(Map<String, String> options, String key, String envName, String defaultValue) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(envName);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String productInfo(Map<String, String> options) throws java.io.IOException {
        String inline = option(options, "product-info", "PRODUCT_INFO", null);
        if (inline != null && !inline.isBlank()) {
            return inline;
        }
        String file = option(options, "product-info-file", "PRODUCT_INFO_FILE", null);
        return file == null || file.isBlank() ? null : Files.readString(Path.of(file));
    }

    private static List<Path> paths(Map<String, String> options, String key, String envName) {
        String value = option(options, key, envName, "");
        if (value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(path -> !path.isBlank())
                .map(Path::of)
                .toList();
    }

    private static Path outputFile(Map<String, String> options) {
        String value = option(options, "output-file", "OUTPUT_FILE", null);
        return value == null || value.isBlank() ? null : Path.of(value);
    }

    private static String usage() {
        return """
                Usage:
                  mvn -pl solution-ecommerce-worker exec:java \\
                    -Dexec.mainClass=com.example.solution.ecommerce.worker.EcommerceAssetWorkflowRunner \\
                    -Dexec.args="--product-info-file /path/to/product-info.txt --product-images /path/p1.png,/path/p2.png --model-images /path/m1.png --output-file /tmp/ecommerce-result.json"

                Required services:
                  - Temporal local service on localhost:7233
                  - model-gateway-service on MODEL_GATEWAY_BASE_URL, default http://localhost:9050

                Options:
                  --tenant-id           Default: demo-tenant
                  --project-id          Default: demo-project
                  --product-info        Inline product info text
                  --product-info-file   File containing product info text
                  --product-images      Required comma-separated product image paths
                  --model-images        Optional comma-separated model image paths
                  --output-file         Optional output file or directory. A timestamp is appended automatically.
                """;
    }

    private record RunConfig(
            String tenantId,
            String projectId,
            String productInfo,
            List<Path> productImageFiles,
            List<Path> modelImageFiles,
            Path outputFile
    ) {
        private static RunConfig from(Map<String, String> options) throws java.io.IOException {
            return new RunConfig(
                    option(options, "tenant-id", "MODEL_GATEWAY_TENANT_ID", "demo-tenant"),
                    option(options, "project-id", "MODEL_GATEWAY_PROJECT_ID", "demo-project"),
                    EcommerceAssetWorkflowRunner.productInfo(options),
                    paths(options, "product-images", "PRODUCT_IMAGES"),
                    paths(options, "model-images", "MODEL_IMAGES"),
                    EcommerceAssetWorkflowRunner.outputFile(options));
        }
    }
}
