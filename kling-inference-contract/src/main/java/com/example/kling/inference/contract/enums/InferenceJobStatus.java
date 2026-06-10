package com.example.kling.inference.contract.enums;

public enum InferenceJobStatus {
    CREATED,
    VALIDATING,
    QUEUED,
    SCHEDULING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    TIMEOUT;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED || this == TIMEOUT;
    }
}
