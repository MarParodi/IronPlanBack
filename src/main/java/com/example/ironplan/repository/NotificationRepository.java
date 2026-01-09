package com.example.ironplan.repository;

import com.example.ironplan.model.Notification;
import com.example.ironplan.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Obtener notificaciones del usuario ordenadas por fecha (más recientes primero)
    Page<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Obtener solo las no leídas
    Page<Notification> findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Filtrar por tipo
    Page<Notification> findByUser_IdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type, Pageable pageable);

    // Filtrar por tipo y no leídas
    Page<Notification> findByUser_IdAndTypeAndIsReadFalseOrderByCreatedAtDesc(
            Long userId, NotificationType type, Pageable pageable);

    // Contar no leídas (para el badge)
    long countByUser_IdAndIsReadFalse(Long userId);

    // Marcar todas como leídas
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    // Eliminar todas las notificaciones del usuario
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    // Verificar que una notificación pertenece al usuario
    boolean existsByIdAndUser_Id(Long id, Long userId);
}
