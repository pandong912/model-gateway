package com.example.modelgateway.core.service;

import com.example.modelgateway.core.model.PromptTemplate;
import java.util.List;
import java.util.Optional;

public interface PromptTemplateRepository {
    List<PromptTemplate> findAll();

    Optional<PromptTemplate> find(String key, String version);

    Optional<PromptTemplate> findLatest(String key);

    Optional<PromptTemplate> findDefaultForScenario(String scenario, String locale);

    PromptTemplate save(PromptTemplate template);
}
