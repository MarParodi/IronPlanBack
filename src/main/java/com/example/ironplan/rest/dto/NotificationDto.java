package com.example.ironplan.rest.dto;

import java.time.LocalDateTime;

public record NotificationDto(
    Long id,
    String type,
    String priority,
    String title,
    String message,
    String routeUrl,
    boolean isRead,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {}
