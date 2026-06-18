package com.example.innowise_vitali_api_gateway.service;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthGrpcClient {

    @GrpcClient("auth-service")
    private AuthGrpcServiceGrpc.AuthGrpcServiceBlockingStub authStub;

    public ValidateTokenResponse validateToken(String token) {
        try {
            ValidateTokenRequest request = ValidateTokenRequest.newBuilder()
                    .setToken(token)
                    .build();
            return authStub.validateToken(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC call to auth-service failed: {} - {}", e.getStatus(), e.getMessage());
            throw new RuntimeException("Auth service unavailable", e);
        }
    }
}