package com.example.ironplan.rest.dto;

import java.util.List;

public record ReorderSessionsRequest(
        Long routineId,
        Integer blockNumber,
        List<Long> sessionIds  // IDs de las sesiones en el nuevo orden
) {}

