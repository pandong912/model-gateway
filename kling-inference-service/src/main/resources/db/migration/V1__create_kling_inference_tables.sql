CREATE TABLE IF NOT EXISTS kling_inference_jobs (
    job_id VARCHAR(128) PRIMARY KEY,
    request_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(256),
    caller_id VARCHAR(128),
    caller_type VARCHAR(64),
    tenant_id VARCHAR(128),
    project_id VARCHAR(128),
    user_id VARCHAR(128),
    generation_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    progress INT NOT NULL,
    backend_task_id VARCHAR(256),
    backend_provider VARCHAR(128),
    trace_id VARCHAR(128) NOT NULL,
    request_payload TEXT NOT NULL,
    result_payload TEXT,
    error_payload TEXT,
    metadata TEXT NOT NULL,
    estimated_wait_seconds INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_kling_jobs_caller_idempotency
    ON kling_inference_jobs (caller_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_kling_jobs_backend_task
    ON kling_inference_jobs (backend_task_id);

CREATE INDEX IF NOT EXISTS idx_kling_jobs_status_updated
    ON kling_inference_jobs (status, updated_at);

CREATE TABLE IF NOT EXISTS kling_inference_events (
    event_id VARCHAR(128) PRIMARY KEY,
    job_id VARCHAR(128) NOT NULL REFERENCES kling_inference_jobs (job_id),
    event_type VARCHAR(64) NOT NULL,
    status VARCHAR(32),
    progress INT,
    result_payload TEXT,
    error_payload TEXT,
    metadata TEXT NOT NULL,
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_kling_events_job_time
    ON kling_inference_events (job_id, occurred_at);
