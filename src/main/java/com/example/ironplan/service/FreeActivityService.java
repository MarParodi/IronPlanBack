package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.FreeActivitySessionRepository;
import com.example.ironplan.repository.UserActivityRepository;
import com.example.ironplan.rest.dto.CreateFreeActivityRequest;
import com.example.ironplan.rest.dto.FreeActivityResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FreeActivityService {

    private final FreeActivitySessionRepository repo;
    private final UserActivityRepository activityRepository;

    public FreeActivityService(
            FreeActivitySessionRepository repo,
            UserActivityRepository activityRepository
    ) {
        this.repo = repo;
        this.activityRepository = activityRepository;
    }

    @Transactional
    public FreeActivityResponse create(User user, CreateFreeActivityRequest req) {
        LocalDateTime completedAt = LocalDateTime.now();
        int duration = req.durationSeconds();
        LocalDateTime startedAt = completedAt.minusSeconds(duration);

        FreeActivitySession session = FreeActivitySession.builder()
                .user(user)
                .activityType(req.activityType())
                .activityTypeOther(req.activityTypeOther())
                .distanceKm(req.distanceKm())
                .durationSeconds(duration)
                .photoUrl(req.photoUrl())
                .notes(req.notes())
                .caloriesEstimated(req.caloriesEstimated() != null
                        ? req.caloriesEstimated()
                        : estimateCalories(req.activityType(), duration))
                .startedAt(startedAt)
                .completedAt(completedAt)
                .build();

        session = repo.save(session);
        recordActivities(user, session);

        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<FreeActivityResponse> listMine(User user) {
        return repo.findByUser_IdOrderByCompletedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private void recordActivities(User user, FreeActivitySession session) {
        LocalDate day = session.getCompletedAt().toLocalDate();
        long sourceId = session.getId();
        double minutes = session.getDurationSeconds() / 60.0;

        saveActivity(user, day, MetricType.SESSIONS, 1.0, sourceId);
        saveActivity(user, day, MetricType.WORKOUTS_COUNT, 1.0, sourceId);
        saveActivity(user, day, MetricType.FREE_ACTIVITY_COUNT, 1.0, sourceId);

        if (minutes > 0) {
            saveActivity(user, day, MetricType.ACTIVE_MINUTES, minutes, sourceId);
        }
        if (session.getDistanceKm() != null && session.getDistanceKm() > 0) {
            saveActivity(user, day, MetricType.FREE_ACTIVITY_KM, session.getDistanceKm(), sourceId);
        }
    }

    private void saveActivity(User user, LocalDate day, MetricType type, double value, Long sourceId) {
        if (activityRepository.existsBySourceIdAndMetricType(sourceId, type)) {
            return;
        }
        activityRepository.save(UserActivity.builder()
                .user(user)
                .activityDate(day)
                .metricType(type)
                .metricValue(value)
                .sourceId(sourceId)
                .build());
    }

    private int estimateCalories(FreeActivityType type, int durationSeconds) {
        double minutes = durationSeconds / 60.0;
        double factor = switch (type) {
            case RUNNING -> 10.0;
            case BOX -> 10.0;
            case FUTBOL -> 9.0;
            case NATACION -> 9.0;
            case BICICLETA_ESTATICA -> 7.0;
            case ELIPTICA -> 7.0;
            case CLASE_GRUPAL -> 7.0;
            case BAILE -> 6.5;
            case CAMINADORA -> 5.0;
            case CAMINATA -> 4.5;
            case YOGA -> 3.5;
            case OTRA -> 6.0;
        };
        return (int) Math.round(minutes * factor);
    }

    private FreeActivityResponse toResponse(FreeActivitySession s) {
        return new FreeActivityResponse(
                s.getId(),
                s.getActivityType(),
                s.getActivityTypeOther(),
                s.getDistanceKm(),
                s.getDurationSeconds(),
                s.getPhotoUrl(),
                s.getNotes(),
                s.getCaloriesEstimated(),
                s.getStartedAt(),
                s.getCompletedAt()
        );
    }
}
