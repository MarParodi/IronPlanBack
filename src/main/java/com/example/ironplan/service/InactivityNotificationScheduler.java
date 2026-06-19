package com.example.ironplan.service;

import com.example.ironplan.model.NotificationPriority;
import com.example.ironplan.model.NotificationType;
import com.example.ironplan.model.User;
import com.example.ironplan.repository.UserActivityRepository;
import com.example.ironplan.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class InactivityNotificationScheduler {

    private final UserRepository userRepo;
    private final UserActivityRepository activityRepo;
    private final NotificationService notificationService;

    public InactivityNotificationScheduler(
            UserRepository userRepo,
            UserActivityRepository activityRepo,
            NotificationService notificationService
    ) {
        this.userRepo = userRepo;
        this.activityRepo = activityRepo;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void notifyInactiveUsers() {
        LocalDate threshold = LocalDate.now().minusDays(3);
        List<User> users = userRepo.findAll();
        for (User user : users) {
            LocalDate last = activityRepo.findLastActivityDate(user.getId());
            if (last != null && !last.isAfter(threshold)) {
                notificationService.createNotification(
                        user,
                        NotificationType.WARNING,
                        NotificationPriority.MEDIUM,
                        "¡El ranking no espera!",
                        "Han pasado 3+ días sin entrenar. Registra tu sesión de hoy.",
                        "/academia"
                );
            }
        }
    }
}
