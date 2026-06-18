package com.example.innowise_vitali_api_gateway.service;

import com.example.innowise_vitali_api_gateway.dto.AuthRegisterRequest;
import com.example.innowise_vitali_api_gateway.dto.RegisterGatewayRequest;
import com.example.innowise_vitali_api_gateway.dto.UserCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class RegistrationOrchestrator {

    private final WebClient authWebClient;
    private final WebClient userWebClient;
    private final String internalSecret;

    public RegistrationOrchestrator(
            @Qualifier("authWebClient") WebClient authWebClient,
            @Qualifier("userWebClient") WebClient userWebClient,
            @Value("${gateway.internal-secret}") String internalSecret) {
        this.authWebClient = authWebClient;
        this.userWebClient = userWebClient;
        this.internalSecret = internalSecret;
    }

    public Mono<Object> register(RegisterGatewayRequest request) {
        log.info("Starting registration orchestration for username={}", request.getUsername());

        AuthRegisterRequest authRequest = AuthRegisterRequest.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

        return callAuthService(authRequest)
                .flatMap(authResponse -> {
                    log.info("Auth service registration succeeded for username={}", request.getUsername());

                    UserCreateRequest userRequest = UserCreateRequest.builder()
                            .username(request.getUsername())
                            .password(request.getPassword())
                            .name(request.getName())
                            .surname(request.getSurname())
                            .email(request.getEmail())
                            .birthDate(request.getBirthDate())
                            .build();

                    return callUserService(userRequest)
                            .doOnSuccess(r -> log.info("User service registration succeeded for username={}", request.getUsername()))
                            .onErrorResume(userError -> {
                                log.error("User service failed for username={}, initiating rollback. Error: {}",
                                        request.getUsername(), userError.getMessage());
                                return rollbackAuthRegistration(request.getUsername())
                                        .then(Mono.error(toResponseStatusException(userError, "User service registration failed")));
                            });
                })
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException) return Mono.error(e);
                    log.error("Auth service failed for username={}: {}", request.getUsername(), e.getMessage());
                    return Mono.error(toResponseStatusException(e, "Auth service registration failed"));
                });
    }

    private Mono<Object> callAuthService(AuthRegisterRequest request) {
        return authWebClient.post()
                .uri("/api/v1/auth/register")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new ResponseStatusException(response.statusCode(), "Auth service error: " + body)
                                ))
                )
                .bodyToMono(Object.class);
    }

    private Mono<Object> callUserService(UserCreateRequest request) {
        return userWebClient.post()
                .uri("/api/v1/users")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(
                                        new ResponseStatusException(response.statusCode(), "User service error: " + body)
                                ))
                )
                .bodyToMono(Object.class);
    }

    private Mono<Void> rollbackAuthRegistration(String username) {
        return authWebClient.delete()
                .uri("/api/v1/auth/internal/rollback/{username}", username)
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Rollback succeeded for username={}", username))
                .onErrorResume(e -> {
                    log.error("CRITICAL: Rollback failed for username={}: {}. Manual cleanup required!", username, e.getMessage());
                    return Mono.empty();
                });
    }

    private ResponseStatusException toResponseStatusException(Throwable e, String defaultMessage) {
        if (e instanceof ResponseStatusException rse) return rse;
        if (e instanceof WebClientResponseException wce) {
            return new ResponseStatusException(wce.getStatusCode(), wce.getResponseBodyAsString());
        }
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, defaultMessage);
    }
}