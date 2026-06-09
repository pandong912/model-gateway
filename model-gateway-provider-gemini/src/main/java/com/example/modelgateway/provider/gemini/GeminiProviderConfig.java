package com.example.modelgateway.provider.gemini;

import java.net.URI;
import java.time.Duration;

public record GeminiProviderConfig(
        String id,
        URI baseUrl,
        String apiKey,
        Duration timeout
) {
}
