// src/main/java/com/example/ironplan/service/XpService.java
package com.example.ironplan.service;

import com.example.ironplan.model.*;
import com.example.ironplan.repository.UserRepository;
import com.example.ironplan.repository.UserXpEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class XpService {

    private final UserRepository userRepo;
    private final UserXpEventRepository xpEventRepo;

    public XpService(UserRepository userRepo, UserXpEventRepository xpEventRepo) {
        this.userRepo = userRepo;
        this.xpEventRepo = xpEventRepo;
    }

    /** Gana XP (workout, logro, etc.) */
    @Transactional
    public void grantXp(User user, int amount, XpEventType type, String description) {
        if (amount <= 0) return;

        // actualizar saldos
        user.setXpPoints(user.getXpPoints() + amount);
        user.setLifetimeXp(user.getLifetimeXp() + amount);

        // recalcular rango
        XpRank newRank = XpRank.fromLifetimeXp(user.getLifetimeXp());
        user.setXpRank(newRank);

        userRepo.save(user);

        // registrar evento
        var event = new UserXpEvent();
        event.setUser(user);
        event.setXpDelta(amount);
        event.setType(type);
        event.setDescription(description);
        xpEventRepo.save(event);
    }

    /** Gasta XP (ej: compra de rutina XP_UNLOCK) */
    @Transactional
    public void spendXp(User user, int amount, RoutineTemplate template, String description) {
        if (amount <= 0) return;
        if (user.getXpPoints() < amount) {
            throw new IllegalArgumentException("XP insuficiente para realizar esta acción.");
        }

        user.setXpPoints(user.getXpPoints() - amount);
        // lifetimeXp NO se reduce, así el rango no baja
        userRepo.save(user);

        var event = new UserXpEvent();
        event.setUser(user);
        event.setXpDelta(-amount);
        event.setType(XpEventType.ROUTINE_PURCHASE);
        event.setRoutineTemplate(template);
        event.setDescription(description);
        xpEventRepo.save(event);
    }
}
