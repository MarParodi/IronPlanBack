// ProfileResponse.java
package com.example.ironplan.rest.dto;

import java.util.List;

public record ProfileResponse(
        ProfileHeaderDto header,
        ProfileStatsDto stats,
        List<RecentWorkoutDto> recentWorkouts
) {}
