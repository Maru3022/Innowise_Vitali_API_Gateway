package com.example.innowise_vitali_api_gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserCreateRequest {
    private String username;
    private String password;
    private String name;
    private String surname;
    private String email;
    private LocalDate birthDate;
}
