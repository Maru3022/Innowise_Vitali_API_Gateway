package com.example.innowise_vitali_api_gateway.exception;

import com.example.innowise_vitali_api_gateway.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Order(-1)
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();

        HttpStatus status;
        String message;
        String error;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : rse.getMessage();
            error = status.getReasonPhrase();
        } else if (ex instanceof WebExchangeBindException bindEx) {
            status = HttpStatus.BAD_REQUEST;
            error = HttpStatus.BAD_REQUEST.getReasonPhrase();
            message = bindEx.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        } else {
            log.error("Unhandled exception: {}", ex.getMessage(), ex);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            error = HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
            message = "An unexpected error occurred";
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(exchange.getRequest().getURI().getPath())
                .build();

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return exchange.getResponse().writeWith(Mono.just(bufferFactory.wrap(bytes)));
        } catch (Exception e) {
            return exchange.getResponse().setComplete();
        }
    }
}