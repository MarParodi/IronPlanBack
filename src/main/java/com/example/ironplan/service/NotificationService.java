package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.NotificationRepository;
import com.example.ironplan.rest.dto.NotificationDto;
import com.example.ironplan.rest.dto.NotificationPageResponse;
import com.example.ironplan.rest.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepo;

    public NotificationService(NotificationRepository notificationRepo) {
        this.notificationRepo = notificationRepo;
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
    public Notification notifyWorkoutCompleted(User user, String routineName, int xpEarned) {
        return createNotification(
                user,
                NotificationType.SUCCESS,
                NotificationPriority.HIGH,
                "Rutina completada 🎉",
                String.format("Terminaste '%s'. Ganaste +%d XP.", routineName, xpEarned),
                "/workouts/last"
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
