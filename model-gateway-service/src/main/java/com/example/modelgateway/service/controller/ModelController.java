package com.example.modelgateway.service.controller;

import com.example.modelgateway.api.model.ModelDescriptor;
import com.example.modelgateway.api.model.ModelRouteDescriptor;
import com.example.modelgateway.core.model.ModelRoute;
import com.example.modelgateway.core.route.RouteRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/models")
public class ModelController {
    private final RouteRepository routeRepository;

    public ModelController(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @GetMapping
    public List<ModelDescriptor> models() {
        return routeRepository.findAll().stream()
                .map(route -> new ModelDescriptor(
                        route.id(),
                        route.provider(),
                        route.model(),
                        route.capabilities(),
                        route.capabilities().stream().anyMatch(capability -> "STREAMING".equals(capability.name())),
                        route.enabled(),
                        route.metadata()))
                .toList();
    }

    @GetMapping("/{id}")
    public ModelRouteDescriptor model(@PathVariable String id) {
        return routeRepository.findById(id)
                .map(ModelRoute::toDescriptor)
                .orElseThrow(() -> new IllegalArgumentException("Model route not found: " + id));
    }
}
