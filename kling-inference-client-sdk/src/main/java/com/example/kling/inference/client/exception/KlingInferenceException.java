package com.example.kling.inference.client.exception;

public class KlingInferenceException extends RuntimeException {

    public KlingInferenceException(String message) {
        super(message);
    }

    public KlingInferenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
