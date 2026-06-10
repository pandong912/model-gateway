package com.example.modelgateway.service.controller;

import com.example.modelgateway.api.model.ModelRouteDescriptor;
import com.example.modelgateway.core.model.ModelRoute;
import com.example.modelgateway.core.route.RouteRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/model-routes")
@RequiredArgsConstructor
public class AdminModelRouteController {
    private final RouteRepository routeRepository;

    @GetMapping
    public List<ModelRouteDescriptor> routes() {
        return routeRepository.findAll().stream()
                .map(route -> route.toDescriptor())
                .toList();
    }

    @PostMapping
    public ModelRouteDescriptor create(@RequestBody ModelRouteDescriptor request) {
        return routeRepository.save(toRoute(request.id(), request)).toDescriptor();
    }

    @PatchMapping("/{id}")
    public ModelRouteDescriptor update(@PathVariable String id, @RequestBody ModelRouteDescriptor request) {
        return routeRepository.save(toRoute(id, request)).toDescriptor();
    }

    private ModelRoute toRoute(String id, ModelRouteDescriptor descriptor) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Route id is required");
        }
        return new ModelRoute(
                id,
                descriptor.provider(),
                descriptor.model(),
                descriptor.capabilities() == null ? List.of() : descriptor.capabilities(),
                descriptor.scenarios() == null ? List.of() : descriptor.scenarios(),
                descriptor.priority(),
                descriptor.enabled(),
                descriptor.fallbackRouteIds() == null ? List.of() : descriptor.fallbackRouteIds(),
                Duration.ofSeconds(60),
                descriptor.metadata() == null ? Map.of() : descriptor.metadata());
    }
}
