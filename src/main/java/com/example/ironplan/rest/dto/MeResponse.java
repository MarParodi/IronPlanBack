package com.example.ironplan.rest.dto;

import com.example.ironplan.model.Gender;
import com.example.ironplan.model.Level;
import com.example.ironplan.model.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MeResponse(
        Long id,
        String email,
        String username,
        Role role,
        LocalDate birthday,
        Integer xpPoints,
        Level level,
        Integer trainDays,
        Gender gender,
        LocalDateTime createdAt
) {}
