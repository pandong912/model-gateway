CREATE TABLE IF NOT EXISTS model_routes (
    id VARCHAR(128) PRIMARY KEY,
    provider VARCHAR(128) NOT NULL,
    model VARCHAR(256) NOT NULL,
    capabilities TEXT NOT NULL,
    scenarios TEXT NOT NULL,
    priority INT NOT NULL,
    enabled BOOLEAN NOT NULL,
    fallback_route_ids TEXT NOT NULL,
    timeout_seconds BIGINT NOT NULL,
    metadata TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS prompt_templates (
    prompt_key VARCHAR(256) NOT NULL,
    version VARCHAR(64) NOT NULL,
    scenario VARCHAR(128),
    locale VARCHAR(32),
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    enabled BOOLEAN NOT NULL,
    default_for_scenario BOOLEAN NOT NULL,
    metadata TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (prompt_key, version)
);

CREATE INDEX IF NOT EXISTS idx_prompt_templates_scenario
    ON prompt_templates (scenario, locale, default_for_scenario, enabled);

CREATE TABLE IF NOT EXISTS prompt_template_audit (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prompt_key VARCHAR(256) NOT NULL,
    version VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
