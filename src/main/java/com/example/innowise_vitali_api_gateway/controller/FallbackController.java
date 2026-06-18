package com.example.innowise_vitali_api_gateway.controller;

import com.example.innowise_vitali_api_gateway.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@RestController
public class FallbackController {

    @RequestMapping("/fallback/auth")
    public Mono<ResponseEntity<ErrorResponse>> authFallback(ServerWebExchange exchange) {
        log.warn("Auth service circuit breaker triggered");
        return buildFallbackResponse(HttpStatus.SERVICE_UNAVAILABLE, "Auth service is temporarily unavailable. Please try again later.", exchange);
    }

    @RequestMapping("/fallback/user")
    public Mono<ResponseEntity<ErrorResponse>> userFallback(ServerWebExchange exchange) {
        log.warn("User service circuit breaker triggered");
        return buildFallbackResponse(HttpStatus.SERVICE_UNAVAILABLE, "User service is temporarily unavailable. Please try again later.", exchange);
    }

    private Mono<ResponseEntity<ErrorResponse>> buildFallbackResponse(HttpStatus status, String message, ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message)
                        .path(exchange.getRequest().getURI().getPath())
                        .build()
        ));
    }
}