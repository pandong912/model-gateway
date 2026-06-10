package com.example.modelgateway.service.controller;

import com.example.modelgateway.api.enums.GatewayErrorCode;
import com.example.modelgateway.api.model.ErrorResponse;
import com.example.modelgateway.core.exception.GatewayException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

@Slf4j
@RestControllerAdvice
public class GatewayExceptionHandler {
    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<ErrorResponse> handleGatewayException(GatewayException exception, ServerWebExchange exchange) {
        String traceId = traceId(exchange);
        if (exception.getStatus() >= 500) {
            log.error("Gateway exception traceId={} status={} code={} message={}",
                    traceId, exception.getStatus(), exception.getCode(), exception.getMessage(), exception);
        } else {
            log.warn("Gateway exception traceId={} status={} code={} message={}",
                    traceId, exception.getStatus(), exception.getCode(), exception.getMessage());
        }
        return ResponseEntity.status(exception.getStatus())
                .body(ErrorResponse.of(exception.getCode(), exception.getMessage(), traceId));
    }

    @ExceptionHandler({WebExchangeBindException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, ServerWebExchange exchange) {
        String traceId = traceId(exchange);
        log.warn("Invalid request traceId={} message={}", traceId, exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(GatewayErrorCode.INVALID_REQUEST, exception.getMessage(), traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, ServerWebExchange exchange) {
        String traceId = traceId(exchange);
        log.error("Unexpected exception traceId={} message={}", traceId, exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(GatewayErrorCode.INTERNAL_ERROR, exception.getMessage(), traceId));
    }

    private String traceId(ServerWebExchange exchange) {
        String requestTraceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        return requestTraceId == null || requestTraceId.isBlank() ? exchange.getRequest().getId() : requestTraceId;
    }
}
