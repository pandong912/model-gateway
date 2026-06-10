package com.example.kling.inference.client.exception;

import com.example.kling.inference.contract.model.VideoGenerationJob;

public class KlingInferenceJobException extends KlingInferenceException {

    private final VideoGenerationJob job;

    public KlingInferenceJobException(VideoGenerationJob job) {
        super("Kling inference job ended with status " + job.status() + ": " + job.jobId());
        this.job = job;
    }

    public VideoGenerationJob getJob() {
        return job;
    }
}
