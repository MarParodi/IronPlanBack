package com.example.ironplan.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter @Setter @AllArgsConstructor
public class AuthResp {

    private String token;
    private String tokenType = "Bearer";
    private String role;
    private long expiresAt;
}
