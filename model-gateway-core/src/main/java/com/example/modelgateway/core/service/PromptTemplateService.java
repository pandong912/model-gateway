package com.example.modelgateway.core.service;

import com.example.modelgateway.api.enums.MessageRole;
import com.example.modelgateway.api.model.ChatCompletionRequest;
import com.example.modelgateway.api.model.ChatMessage;
import com.example.modelgateway.core.model.ModelRoute;
import com.example.modelgateway.core.model.PromptTemplate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PromptTemplateService {
    public static final String PROMPT_KEY = "promptKey";
    public static final String PROMPT_VERSION = "promptVersion";
    public static final String PROMPT_LOCALE = "promptLocale";
    public static final String PROMPT_VARIABLES = "promptVariables";
    public static final String RESOLVED_PROMPT_KEY = "resolvedPromptKey";
    public static final String RESOLVED_PROMPT_VERSION = "resolvedPromptVersion";

    private final PromptTemplateRepository repository;

    public ChatCompletionRequest apply(ChatCompletionRequest request, ModelRoute route) {
        Optional<PromptTemplate> template = resolveTemplate(request);
        if (template.isEmpty()) {
            return request;
        }

        PromptTemplate resolved = template.get();
        Map<String, Object> variables = variables(request, route);
        String content = render(resolved.content(), variables);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(
                resolved.role() == null ? MessageRole.SYSTEM : resolved.role(),
                content,
                null,
                List.of(),
                Map.of(RESOLVED_PROMPT_KEY, resolved.key(), RESOLVED_PROMPT_VERSION, resolved.version())));
        messages.addAll(request.messages());

        Map<String, Object> metadata = new HashMap<>(request.safeMetadata());
        metadata.put(RESOLVED_PROMPT_KEY, resolved.key());
        metadata.put(RESOLVED_PROMPT_VERSION, resolved.version());

        return new ChatCompletionRequest(
                request.tenantId(),
                request.projectId(),
                request.scenario(),
                request.capability(),
                request.modelHint(),
                messages,
                request.parameters(),
                request.stream(),
                metadata);
    }

    public String render(String template, Map<String, Object> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }
        String rendered = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return rendered;
    }

    private Optional<PromptTemplate> resolveTemplate(ChatCompletionRequest request) {
        Map<String, Object> metadata = request.safeMetadata();
        String promptKey = stringValue(metadata.get(PROMPT_KEY));
        String promptVersion = stringValue(metadata.get(PROMPT_VERSION));
        String promptLocale = stringValue(metadata.get(PROMPT_LOCALE));

        if (promptKey != null && promptVersion != null) {
            return repository.find(promptKey, promptVersion);
        }
        if (promptKey != null) {
            return repository.findLatest(promptKey);
        }
        return repository.findDefaultForScenario(request.scenario(), promptLocale);
    }

    private Map<String, Object> variables(ChatCompletionRequest request, ModelRoute route) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("tenantId", request.tenantId());
        variables.put("projectId", request.projectId());
        variables.put("scenario", request.scenario());
        variables.put("provider", route.provider());
        variables.put("model", route.model());
        variables.put("routeId", route.id());

        Object configuredVariables = request.safeMetadata().get(PROMPT_VARIABLES);
        if (configuredVariables instanceof Map<?, ?> variableMap) {
            variableMap.forEach((key, value) -> variables.put(String.valueOf(key), value));
        }
        return variables;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }
}
