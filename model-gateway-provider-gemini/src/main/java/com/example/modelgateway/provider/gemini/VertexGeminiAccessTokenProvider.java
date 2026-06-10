package com.example.modelgateway.provider.gemini;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class VertexGeminiAccessTokenProvider {
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/cloud-platform");

    private final Path credentialsPath;
    private GoogleCredentials credentials;

    public VertexGeminiAccessTokenProvider(String credentialsPath) {
        this.credentialsPath = credentialsPath == null || credentialsPath.isBlank() ? null : Path.of(credentialsPath);
    }

    public synchronized String accessToken() throws IOException {
        if (credentialsPath == null) {
            throw new IOException("Vertex Gemini credentials path must be configured");
        }
        if (credentials == null) {
            try (InputStream input = Files.newInputStream(credentialsPath)) {
                credentials = GoogleCredentials.fromStream(input).createScoped(SCOPES);
            }
        }
        credentials.refreshIfExpired();
        AccessToken token = credentials.getAccessToken();
        if (token == null || token.getTokenValue() == null || token.getTokenValue().isBlank()) {
            credentials.refresh();
            token = credentials.getAccessToken();
        }
        if (token == null || token.getTokenValue() == null || token.getTokenValue().isBlank()) {
            throw new IOException("Failed to resolve Vertex Gemini access token");
        }
        return token.getTokenValue();
    }
}
