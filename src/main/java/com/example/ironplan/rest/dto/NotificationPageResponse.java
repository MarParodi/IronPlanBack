package com.example.ironplan.rest.dto;

import java.util.List;

public record NotificationPageResponse(
    List<NotificationDto> content,
    int totalPages,
    long totalElements,
    int size,
    int number,
    boolean first,
    boolean last,
    long unreadCount
) {}
