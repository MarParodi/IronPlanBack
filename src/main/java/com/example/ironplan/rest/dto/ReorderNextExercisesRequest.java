// ReorderNextExercisesRequest.java
package com.example.ironplan.rest.dto;

import java.util.List;

public record ReorderNextExercisesRequest(
        List<Long> workoutExerciseIds
) {}
