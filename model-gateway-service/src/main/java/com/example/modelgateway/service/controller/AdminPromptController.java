package com.example.modelgateway.service.controller;

import com.example.modelgateway.core.model.PromptTemplate;
import com.example.modelgateway.core.service.PromptTemplateRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/v1/prompts")
public class AdminPromptController {
    private final PromptTemplateRepository repository;

    public AdminPromptController(PromptTemplateRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PromptTemplate> prompts() {
        return repository.findAll();
    }

    @PostMapping
    public PromptTemplate create(@RequestBody PromptTemplate promptTemplate) {
        return repository.save(promptTemplate);
    }

    @PatchMapping("/{key}/{version}")
    public PromptTemplate update(
            @PathVariable String key,
            @PathVariable String version,
            @RequestBody PromptTemplate promptTemplate
    ) {
        return repository.save(new PromptTemplate(
                key,
                version,
                promptTemplate.scenario(),
                promptTemplate.locale(),
                promptTemplate.role(),
                promptTemplate.content(),
                promptTemplate.enabled(),
                promptTemplate.defaultForScenario(),
                promptTemplate.metadata()));
    }
}
