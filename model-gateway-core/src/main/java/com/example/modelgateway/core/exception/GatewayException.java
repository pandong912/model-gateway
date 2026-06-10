package com.example.modelgateway.core.exception;

import com.example.modelgateway.api.enums.GatewayErrorCode;
import lombok.Getter;

@Getter
public class GatewayException extends RuntimeException {
    private final GatewayErrorCode code;
    private final int status;

    public GatewayException(GatewayErrorCode code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public GatewayException(GatewayErrorCode code, String message, int status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }
}
