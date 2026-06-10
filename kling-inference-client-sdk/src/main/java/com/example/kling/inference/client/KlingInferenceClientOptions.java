package com.example.kling.inference.client;

import java.time.Duration;

public record KlingInferenceClientOptions(
        String baseUrl,
        String apiKey,
        Duration waitTimeout,
        Duration pollInterval
) {
    public KlingInferenceClientOptions {
        if (waitTimeout == null) {
            waitTimeout = Duration.ofSeconds(30);
        }
        if (pollInterval == null) {
            pollInterval = Duration.ofMillis(500);
        }
    }

    public static KlingInferenceClientOptions of(String baseUrl) {
        return new KlingInferenceClientOptions(baseUrl, null, Duration.ofSeconds(30), Duration.ofMillis(500));
    }
}
