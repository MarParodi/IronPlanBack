package com.example.ironplan.rest;

import com.example.ironplan.model.NotificationType;
import com.example.ironplan.model.User;
import com.example.ironplan.rest.dto.NotificationCountResponse;
import com.example.ironplan.rest.dto.NotificationDto;
import com.example.ironplan.rest.dto.NotificationPageResponse;
import com.example.ironplan.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Obtener notificaciones del usuario con paginación y filtros
     * GET /api/notifications?page=0&size=20&unreadOnly=false&type=SUCCESS
     */
    @GetMapping
    public NotificationPageResponse getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) NotificationType type
    ) {
        return notificationService.getNotifications(user.getId(), unreadOnly, type, page, size);
    }

    /**
     * Obtener solo el conteo de no leídas (para badge)
     * GET /api/notifications/count
     */
    @GetMapping("/count")
    public NotificationCountResponse getUnreadCount(@AuthenticationPrincipal User user) {
        long count = notificationService.getUnreadCount(user.getId());
        return new NotificationCountResponse(count);
    }

    /**
     * Marcar una notificación como leída
     * PATCH /api/notifications/{id}/read
     */
    @PatchMapping("/{id}/read")
    public NotificationDto markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        return notificationService.markAsRead(user.getId(), id);
    }

    /**
     * Marcar una notificación como no leída
     * PATCH /api/notifications/{id}/unread
     */
    @PatchMapping("/{id}/unread")
    public NotificationDto markAsUnread(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        return notificationService.markAsUnread(user.getId(), id);
    }

    /**
     * Marcar todas las notificaciones como leídas
     * POST /api/notifications/mark-all-read
     */
    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Eliminar una notificación
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        notificationService.deleteNotification(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Eliminar todas las notificaciones
     * DELETE /api/notifications
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications(@AuthenticationPrincipal User user) {
        notificationService.deleteAll(user.getId());
        return ResponseEntity.noContent().build();
    }
}
