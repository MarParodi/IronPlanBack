package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_unlocked_routines",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "routine_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUnlockedRoutine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private RoutineTemplate routine;

    private LocalDateTime unlockedAt = LocalDateTime.now();


}