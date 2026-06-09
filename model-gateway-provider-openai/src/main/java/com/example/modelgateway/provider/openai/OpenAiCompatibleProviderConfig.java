package com.example.modelgateway.provider.openai;

import java.net.URI;
import java.time.Duration;

public record OpenAiCompatibleProviderConfig(
        String id,
        URI baseUrl,
        String apiKey,
        String organization,
        Duration timeout
) {
}
