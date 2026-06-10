package com.example.kling.inference.client.exception;

import com.example.kling.inference.contract.model.KlingGenerationJob;

public class KlingInferenceJobException extends KlingInferenceException {

    private final KlingGenerationJob job;

    public KlingInferenceJobException(KlingGenerationJob job) {
        super("Kling inference job ended with status " + job.status() + ": " + job.jobId());
        this.job = job;
    }

    public KlingGenerationJob getJob() {
        return job;
    }
}
