package com.example.innowise_vitali_api_gateway.controller;


import com.example.innowise_vitali_api_gateway.dto.RegisterGatewayRequest;
import com.example.innowise_vitali_api_gateway.service.RegistrationOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationOrchestrator registrationOrchestrator;

    @PostMapping("/register")
    public Mono<ResponseEntity<Object>> register(@Valid @RequestBody RegisterGatewayRequest request) {
        return registrationOrchestrator.register(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
}