package com.example.ironplan.rest.dto;

public record RegisterStep1Resp(
        String onboardingToken,
        long expiresAt
) {}

