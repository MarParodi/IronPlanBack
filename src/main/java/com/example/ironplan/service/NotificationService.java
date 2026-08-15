package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.NotificationRepository;
import com.example.ironplan.repository.ProgressRepository;
import com.example.ironplan.repository.UserRepository;
import com.example.ironplan.rest.dto.NotificationDto;
import com.example.ironplan.rest.dto.NotificationPageResponse;
import com.example.ironplan.rest.dto.progress.ProgressionRecommendationDto;
import com.example.ironplan.rest.dto.progress.RecentPerformanceDto;
import com.example.ironplan.rest.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final int REST_STREAK_THRESHOLD = 5;
    private static final int REST_NOTIFICATION_COOLDOWN_DAYS = 7;
    private static final int PROGRESSION_NOTIFICATION_COOLDOWN_DAYS = 3;

    private final NotificationRepository notificationRepo;
    private final ProgressRepository progressRepo;
    private final UserRepository userRepo;

    public NotificationService(
            NotificationRepository notificationRepo,
            ProgressRepository progressRepo,
            UserRepository userRepo
    ) {
        this.notificationRepo = notificationRepo;
        this.progressRepo = progressRepo;
        this.userRepo = userRepo;
    }

    // ================== CONSULTAS ==================

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(
            Long userId,
            Boolean unreadOnly,
            NotificationType type,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage;

        if (type != null && Boolean.TRUE.equals(unreadOnly)) {
            notificationPage = notificationRepo.findByUser_IdAndTypeAndIsReadFalseOrderByCreatedAtDesc(
                    userId, type, pageable);
        } else if (type != null) {
            notificationPage = notificationRepo.findByUser_IdAndTypeOrderByCreatedAtDesc(
                    userId, type, pageable);
        } else if (Boolean.TRUE.equals(unreadOnly)) {
            notificationPage = notificationRepo.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(
                    userId, pageable);
        } else {
            notificationPage = notificationRepo.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
        }

        long unreadCount = notificationRepo.countByUser_IdAndIsReadFalse(userId);

        List<NotificationDto> dtos = notificationPage.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new NotificationPageResponse(
                dtos,
                notificationPage.getTotalPages(),
                notificationPage.getTotalElements(),
                notificationPage.getSize(),
                notificationPage.getNumber(),
                notificationPage.isFirst(),
                notificationPage.isLast(),
                unreadCount
        );
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepo.countByUser_IdAndIsReadFalse(userId);
    }

    // ================== ACCIONES ==================

    @Transactional
    public NotificationDto markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notificación no encontrada"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new NotFoundException("Notificación no encontrada");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepo.save(notification);
        }

        return toDto(notification);
    }

    @Transactional
    public NotificationDto markAsUnread(Long userId, Long notificationId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notificación no encontrada"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new NotFoundException("Notificación no encontrada");
        }

        notification.setRead(false);
        notification.setReadAt(null);
        notificationRepo.save(notification);

        return toDto(notification);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepo.markAllAsReadByUserId(userId);
    }

    @Transactional
    public int deleteAll(Long userId) {
        return notificationRepo.deleteAllByUserId(userId);
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        if (!notificationRepo.existsByIdAndUser_Id(notificationId, userId)) {
            throw new NotFoundException("Notificación no encontrada");
        }
        notificationRepo.deleteById(notificationId);
    }

    // ================== CREAR NOTIFICACIONES ==================

    @Transactional
    public Notification createNotification(
            User user,
            NotificationType type,
            NotificationPriority priority,
            String title,
            String message,
            String routeUrl
    ) {
        Notification notification = new Notification(user, type, priority, title, message, routeUrl);
        return notificationRepo.save(notification);
    }

    // Métodos de conveniencia para crear notificaciones específicas
    public Notification notifyWorkoutCompleted(User user, Long sessionId, String routineName, int xpEarned) {
        String routeUrl = "/workouts/" + sessionId + "/summary";
        return createNotification(
                user,
                NotificationType.SUCCESS,
                NotificationPriority.HIGH,
                "Rutina completada 🎉",
                String.format("Terminaste '%s'. Ganaste +%d XP.", routineName, xpEarned),
                routeUrl
        );
    }

    public Notification notifyProgressionSuggestion(User user, String exerciseName, 
                                                     double lastWeight, int lastReps,
                                                     double suggestedWeight) {
        return createNotification(
                user,
                NotificationType.INFO,
                NotificationPriority.MEDIUM,
                "Sugerencia de progresión 📈",
                String.format("%s: la última vez %.1f kg × %d. Hoy prueba %.1f kg.", 
                        exerciseName, lastWeight, lastReps, suggestedWeight),
                "/mis-rutinas"
        );
    }

    public Notification notifyRestRecommendation(User user, int consecutiveDays) {
        return createNotification(
                user,
                NotificationType.WARNING,
                NotificationPriority.MEDIUM,
                "Descanso recomendado ⚠️",
                String.format("Llevas %d días seguidos entrenando. Considera un día ligero o descanso.", 
                        consecutiveDays),
                "/perfil/estadisticas"
        );
    }

    public Notification notifyNewRoutineAvailable(User user, String routineName) {
        return createNotification(
                user,
                NotificationType.INFO,
                NotificationPriority.LOW,
                "Nueva rutina disponible 🆕",
                String.format("Se publicó '%s' en la comunidad.", routineName),
                "/"
        );
    }

    public Notification notifyAchievementUnlocked(User user, String achievementName, int xpReward) {
        return createNotification(
                user,
                NotificationType.SUCCESS,
                NotificationPriority.HIGH,
                "¡Hazaña desbloqueada! 🏆",
                String.format("Desbloqueaste '%s'. Ganaste +%d XP.", achievementName, xpReward),
                "/perfil/hazanas"
        );
    }

    public Notification notifyCompetitionWinner(
            User user,
            String competitionName,
            Long competitionId,
            PodiumScope scope,
            String levelLabel
    ) {
        String categoryText = scope == PodiumScope.GENERAL
                ? "categoría General"
                : "categoría " + levelLabel;
        return createNotification(
                user,
                NotificationType.SUCCESS,
                NotificationPriority.HIGH,
                "¡Ganaste el reto! 🏆",
                String.format("¡Felicitaciones! Fuiste seleccionado ganador del reto '%s' en la %s.",
                        competitionName, categoryText),
                "/competitions/" + competitionId
        );
    }

    /**
     * Notifica al completar una sesión de entrenamiento (con deduplicación por sesión).
     */
    @Transactional
    public void handleWorkoutSessionCompleted(WorkoutSession session) {
        if (session == null || session.getUser() == null) return;

        User user = session.getUser();
        String routeUrl = "/workouts/" + session.getId() + "/summary";

        if (!notificationRepo.existsByUser_IdAndRouteUrl(user.getId(), routeUrl)) {
            String routineName = session.getRoutineDetail() != null && session.getRoutineDetail().getTitle() != null
                    ? session.getRoutineDetail().getTitle()
                    : "Entrenamiento";
            int xpEarned = session.getXpEarned() != null ? session.getXpEarned() : 0;
            notifyWorkoutCompleted(user, session.getId(), routineName, xpEarned);
        }

        maybeNotifyRestRecommendation(user);
    }

    /**
     * Sugerencia de progresión cuando el algoritmo recomienda subir peso.
     * Corre en su propia transacción para que un fallo aquí no arrastre al flujo que la invoca.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void maybeNotifyProgressionSuggestion(User user, ProgressionRecommendationDto recommendation) {
        if (user == null || recommendation == null) return;
        if (recommendation.type() != ProgressionRecommendationDto.RecommendationType.INCREASE_WEIGHT) return;
        if (recommendation.suggestedWeightKg() == null) return;

        List<RecentPerformanceDto> recent = recommendation.recentPerformance();
        if (recent == null || recent.isEmpty()) return;

        RecentPerformanceDto last = recent.get(0);
        if (last.weightKg() == null || last.avgReps() == null) return;

        LocalDateTime cooldown = LocalDateTime.now().minusDays(PROGRESSION_NOTIFICATION_COOLDOWN_DAYS);
        if (notificationRepo.existsByUser_IdAndMessageContainingAndCreatedAtAfter(
                user.getId(), recommendation.exerciseName(), cooldown)) {
            return;
        }

        notifyProgressionSuggestion(
                user,
                recommendation.exerciseName(),
                last.weightKg(),
                last.avgReps(),
                recommendation.suggestedWeightKg()
        );
    }

    /**
     * Avisa a la comunidad cuando se publica una rutina pública nueva.
     */
    @Transactional
    public void notifyCommunityAboutNewRoutine(RoutineTemplate routine, User creator) {
        if (routine == null || creator == null || !Boolean.TRUE.equals(routine.getIsPublic())) return;

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        String dedupeFragment = "Se publicó '" + routine.getName() + "'";

        for (User user : userRepo.findAll()) {
            if (user.getId().equals(creator.getId())) continue;
            if (notificationRepo.existsByUser_IdAndMessageContainingAndCreatedAtAfter(
                    user.getId(), dedupeFragment, since)) {
                continue;
            }
            notifyNewRoutineAvailable(user, routine.getName());
        }
    }

    private void maybeNotifyRestRecommendation(User user) {
        List<LocalDate> workoutDates = progressRepo.findWorkoutDates(user.getId());
        int streak = calculateCurrentStreak(workoutDates);
        if (streak < REST_STREAK_THRESHOLD) return;

        LocalDateTime cooldown = LocalDateTime.now().minusDays(REST_NOTIFICATION_COOLDOWN_DAYS);
        if (notificationRepo.existsByUser_IdAndTitleAndCreatedAtAfter(
                user.getId(), "Descanso recomendado", cooldown)) {
            return;
        }

        notifyRestRecommendation(user, streak);
    }

    private int calculateCurrentStreak(List<LocalDate> workoutDates) {
        if (workoutDates == null || workoutDates.isEmpty()) return 0;

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        if (!workoutDates.contains(today) && !workoutDates.contains(yesterday)) {
            return 0;
        }

        int streak = 0;
        LocalDate checkDate = workoutDates.contains(today) ? today : yesterday;

        for (LocalDate date : workoutDates) {
            if (date.equals(checkDate)) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else if (date.isBefore(checkDate)) {
                break;
            }
        }

        return streak;
    }

    // ================== MAPPER ==================

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType().name(),
                n.getPriority().name(),
                n.getTitle(),
                n.getMessage(),
                n.getRouteUrl(),
                n.isRead(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}
