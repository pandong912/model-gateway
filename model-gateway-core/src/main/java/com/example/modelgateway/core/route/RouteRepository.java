package com.example.modelgateway.core.route;

import com.example.modelgateway.core.model.ModelRoute;
import java.util.List;
import java.util.Optional;

public interface RouteRepository {
    List<ModelRoute> findAll();

    Optional<ModelRoute> findById(String id);

    ModelRoute save(ModelRoute route);
}
