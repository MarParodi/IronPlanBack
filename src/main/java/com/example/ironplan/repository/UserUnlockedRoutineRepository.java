package com.example.ironplan.repository;

import com.example.ironplan.model.UserUnlockedRoutine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface UserUnlockedRoutineRepository
        extends JpaRepository<UserUnlockedRoutine, Long> {

    boolean existsByUser_IdAndRoutine_Id(Long userId, Long routineId);

    @Query("SELECT ur.routine.id FROM UserUnlockedRoutine ur WHERE ur.user.id = :userId AND ur.routine.id IN :routineIds")
    Set<Long> findUnlockedRoutineIdsByUserAndRoutineIds(@Param("userId") Long userId, @Param("routineIds") List<Long> routineIds);
}
