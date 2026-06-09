package com.example.modelgateway.core.route;

import com.example.modelgateway.api.enums.GatewayErrorCode;
import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.core.exception.GatewayException;
import com.example.modelgateway.core.model.ModelRoute;
import java.util.Comparator;
import java.util.Optional;

public class ModelRouter {
    private final RouteRepository routeRepository;

    public ModelRouter(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public ModelRoute route(ChatCompletionRequest request) {
        if (request.modelHint() != null && !request.modelHint().isBlank()) {
            Optional<ModelRoute> hinted = routeRepository.findAll().stream()
                    .filter(ModelRoute::enabled)
                    .filter(route -> request.modelHint().equals(route.id()) || request.modelHint().equals(route.model()))
                    .findFirst();
            if (hinted.isPresent()) {
                return hinted.get();
            }
        }

        return routeRepository.findAll().stream()
                .filter(ModelRoute::enabled)
                .filter(route -> route.capabilities().contains(request.resolvedCapability()))
                .filter(route -> matchesScenario(route, request.scenario()))
                .min(Comparator.comparingInt(ModelRoute::priority))
                .orElseThrow(() -> new GatewayException(
                        GatewayErrorCode.ROUTE_NOT_FOUND,
                        "No model route matched capability " + request.resolvedCapability(),
                        404));
    }

    private boolean matchesScenario(ModelRoute route, String scenario) {
        return scenario == null || scenario.isBlank() || route.scenarios().isEmpty() || route.scenarios().contains(scenario);
    }
}
