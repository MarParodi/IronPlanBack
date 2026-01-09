package com.example.ironplan.rest.dto;

public record UserResponseDto(
        Long id,
        String username,
        String email,
        String profilePictureUrl,
        String currentRoutineName
) {
}
