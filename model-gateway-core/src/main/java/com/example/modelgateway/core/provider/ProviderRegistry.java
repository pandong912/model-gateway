package com.example.modelgateway.core.provider;

import com.example.modelgateway.api.enums.GatewayErrorCode;
import com.example.modelgateway.core.exception.GatewayException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProviderRegistry {
    private final Map<String, ProviderClient> providers;

    public ProviderRegistry(Collection<ProviderClient> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(ProviderClient::providerId, Function.identity()));
    }

    public ProviderClient getRequired(String providerId) {
        ProviderClient client = providers.get(providerId);
        if (client == null) {
            throw new GatewayException(GatewayErrorCode.PROVIDER_NOT_FOUND, "Provider not registered: " + providerId, 404);
        }
        return client;
    }
}
