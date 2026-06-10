package com.example.modelgateway.provider.gemini;

import java.net.URI;
import java.time.Duration;

public record VertexGeminiProviderConfig(
        String id,
        URI baseUrl,
        String projectId,
        String location,
        String credentialsPath,
        Duration timeout
) {
}
