package com.example.modelgateway.examples;

import com.example.modelgateway.api.model.ChatCompletionResponse;
import com.example.modelgateway.client.ModelGatewayClient;
import com.example.modelgateway.client.ModelGatewayClientProperties;
import com.example.modelgateway.client.WebClientModelGatewayClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public class EcommerceAssetDesignExampleRunner {
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);
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

            ModelGatewayClientProperties properties = new ModelGatewayClientProperties();
            properties.setBaseUrl(URI.create(config.gatewayBaseUrl()));
            if (config.apiKey() != null && !config.apiKey().isBlank()) {
                properties.setApiKey(config.apiKey());
            }

            ModelGatewayClient client = new WebClientModelGatewayClient(WebClient.builder(), properties);
            EcommerceAssetDesignExample example = new EcommerceAssetDesignExample(client);
            ChatCompletionResponse response = example.generateAssetPlan(
                    config.tenantId(),
                    config.projectId(),
                    new EcommerceAssetDesignExample.UserPromptInput(
                            config.productInfo(),
                            config.productImageFiles(),
                            config.modelImageFiles()))
                    .block(REQUEST_TIMEOUT);

            if (response == null) {
                throw new IllegalStateException("Model gateway returned an empty response");
            }
            writeResponse(response, config.outputFile());
        } catch (WebClientResponseException ex) {
            System.err.println(ex.getMessage());
            String responseBody = ex.getResponseBodyAsString();
            if (responseBody != null && !responseBody.isBlank()) {
                System.err.println(responseBody);
            }
            System.err.println();
            System.err.println(usage());
            System.exit(1);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.err.println();
            System.err.println(usage());
            System.exit(1);
        }
    }

    private static void validate(RunConfig config) {
        if (config.productInfo() == null || config.productInfo().isBlank()) {
            throw new IllegalArgumentException("Missing product info. Use --product-info or --product-info-file.");
        }
        if (config.productImageFiles().isEmpty()) {
            throw new IllegalArgumentException("Missing product images. Use --product-images with comma-separated file paths.");
        }
        config.productImageFiles().forEach(EcommerceAssetDesignExampleRunner::validateFile);
        config.modelImageFiles().forEach(EcommerceAssetDesignExampleRunner::validateFile);
    }

    private static void validateFile(Path path) {
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("Image file is not readable: " + path);
        }
    }

    private static void writeResponse(ChatCompletionResponse response, Path outputFile) throws java.io.IOException {
        if (outputFile == null) {
            System.out.println(response.content());
            return;
        }
        Path resolvedOutputFile = timestampedOutputFile(outputFile);
        Path parent = resolvedOutputFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        OBJECT_MAPPER.writeValue(resolvedOutputFile.toFile(), response);
        System.out.println("Response written to: " + resolvedOutputFile.toAbsolutePath());
        System.out.println("traceId=" + response.traceId()
                + ", provider=" + response.provider()
                + ", model=" + response.model()
                + ", latencyMs=" + response.latencyMs());
    }

    private static Path timestampedOutputFile(Path outputFile) {
        String timestamp = LocalDateTime.now().format(OUTPUT_TIMESTAMP);
        if (Files.isDirectory(outputFile)) {
            return outputFile.resolve("ecommerce-response-" + timestamp + ".json");
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
        if (file == null || file.isBlank()) {
            return null;
        }
        return Files.readString(Path.of(file));
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

    private static String usage() {
        return """
                Usage:
                  mvn -pl model-gateway-examples exec:java \\
                    -Dexec.mainClass=com.example.modelgateway.examples.EcommerceAssetDesignExampleRunner \\
                    -Dexec.args="--product-info-file /path/to/product-info.txt --product-images /path/p1.png,/path/p2.png --model-images /path/m1.png"

                Options:
                  --gateway-base-url    Model Gateway base URL. Default: http://localhost:8080
                  --api-key             Optional X-API-Key when gateway security is enabled
                  --tenant-id           Default: demo-tenant
                  --project-id          Default: demo-project
                  --product-info        Inline product info text
                  --product-info-file   File containing product info text
                  --product-images      Required comma-separated product image paths
                  --model-images        Optional comma-separated model image paths
                  --output-file         Optional output file or directory. A timestamp is appended automatically.

                Environment variables with the same purpose:
                  MODEL_GATEWAY_BASE_URL, MODEL_GATEWAY_API_KEY, PRODUCT_INFO, PRODUCT_INFO_FILE, PRODUCT_IMAGES, MODEL_IMAGES, OUTPUT_FILE
                """;
    }

    private record RunConfig(
            String gatewayBaseUrl,
            String apiKey,
            String tenantId,
            String projectId,
            String productInfo,
            List<Path> productImageFiles,
            List<Path> modelImageFiles,
            Path outputFile
    ) {
        private static RunConfig from(Map<String, String> options) throws java.io.IOException {
            return new RunConfig(
                    option(options, "gateway-base-url", "MODEL_GATEWAY_BASE_URL", "http://localhost:9050"),
                    option(options, "api-key", "MODEL_GATEWAY_API_KEY", null),
                    option(options, "tenant-id", "MODEL_GATEWAY_TENANT_ID", "demo-tenant"),
                    option(options, "project-id", "MODEL_GATEWAY_PROJECT_ID", "demo-project"),
                    EcommerceAssetDesignExampleRunner.productInfo(options),
                    paths(options, "product-images", "PRODUCT_IMAGES"),
                    paths(options, "model-images", "MODEL_IMAGES"),
                    EcommerceAssetDesignExampleRunner.outputFile(options));
        }
    }

    private static Path outputFile(Map<String, String> options) {
        String value = option(options, "output-file", "OUTPUT_FILE", null);
        return value == null || value.isBlank() ? null : Path.of(value);
    }
}
