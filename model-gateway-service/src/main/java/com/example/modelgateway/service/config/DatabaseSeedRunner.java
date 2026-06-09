package com.example.modelgateway.service.config;

import com.example.modelgateway.core.model.ModelRoute;
import com.example.modelgateway.core.route.RouteRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeedRunner implements ApplicationRunner {
    private final ModelGatewayProperties properties;
    private final RouteRepository routeRepository;

    public DatabaseSeedRunner(
            ModelGatewayProperties properties,
            RouteRepository routeRepository
    ) {
        this.properties = properties;
        this.routeRepository = routeRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedRoutes();
    }

    private void seedRoutes() {
        properties.getRoutes().stream()
                .filter(route -> routeRepository.findById(route.getId()).isEmpty())
                .map(route -> new ModelRoute(
                        route.getId(),
                        route.getProvider(),
                        route.getModel(),
                        route.getCapabilities(),
                        route.getScenarios(),
                        route.getPriority(),
                        route.isEnabled(),
                        route.getFallbackRouteIds(),
                        route.getTimeout(),
                        route.getMetadata()))
                .forEach(routeRepository::save);
    }
}
