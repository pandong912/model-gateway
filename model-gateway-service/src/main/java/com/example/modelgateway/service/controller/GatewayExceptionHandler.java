package com.example.modelgateway.service.controller;

import com.example.modelgateway.api.enums.GatewayErrorCode;
import com.example.modelgateway.api.model.ErrorResponse;
import com.example.modelgateway.core.exception.GatewayException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GatewayExceptionHandler {
    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<ErrorResponse> handleGatewayException(GatewayException exception, ServerWebExchange exchange) {
        return ResponseEntity.status(exception.status())
                .body(ErrorResponse.of(exception.code(), exception.getMessage(), traceId(exchange)));
    }

    @ExceptionHandler({WebExchangeBindException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(GatewayErrorCode.INVALID_REQUEST, exception.getMessage(), traceId(exchange)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(GatewayErrorCode.INTERNAL_ERROR, exception.getMessage(), traceId(exchange)));
    }

    private String traceId(ServerWebExchange exchange) {
        String requestTraceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        return requestTraceId == null || requestTraceId.isBlank() ? exchange.getRequest().getId() : requestTraceId;
    }
}
