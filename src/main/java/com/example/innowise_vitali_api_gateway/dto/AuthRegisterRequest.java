package com.example.innowise_vitali_api_gateway.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthRegisterRequest {
    private String username;
    private String email;
    private String password;
}