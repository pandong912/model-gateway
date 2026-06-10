package com.example.modelgateway.examples;

import com.example.modelgateway.client.ModelGatewayClient;
import com.example.modelgateway.core.model.PromptTemplate;
import java.util.Map;
import reactor.core.publisher.Mono;

abstract class PromptTemplateSupport {
    protected final ModelGatewayClient modelGatewayClient;

    protected PromptTemplateSupport(ModelGatewayClient modelGatewayClient) {
        this.modelGatewayClient = modelGatewayClient;
    }

    protected Mono<PromptTemplate> promptTemplate(String key, String version) {
        return modelGatewayClient.prompts()
                .flatMapMany(reactor.core.publisher.Flux::fromIterable)
                .filter(template -> template.key().equals(key))
                .filter(template -> version == null || version.equals(template.version()))
                .next()
                .switchIfEmpty(Mono.error(new IllegalStateException("Prompt template not found: " + key + ":" + version)));
    }

    protected String render(PromptTemplate template, Map<String, ?> variables) {
        String rendered = template.content();
        for (Map.Entry<String, ?> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return rendered;
    }
}
