package com.example.ironplan.repository;
import com.example.ironplan.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);
    
    
    @Query("""
            SELECT u.primaryOrganizationalGroup.id, COUNT(u.id)
            FROM User u
            WHERE u.primaryOrganizationalGroup IS NOT NULL
            GROUP BY u.primaryOrganizationalGroup.id
        """)
        List<Object[]> countMembersByGroupRaw();

        default Map<Long, Integer> countMembersByGroup() {
            return countMembersByGroupRaw().stream()
                .collect(Collectors.toMap(
                    row -> (Long) row[0],
                    row -> ((Long) row[1]).intValue()
                ));
        }
        
        List<User> findByPrimaryOrganizationalGroupId(Long groupId);
        
        int countByPrimaryOrganizationalGroupId(Long groupId);

        List<User> findByPrimaryOrganizationalGroupIdIn(List<Long> groupIds);

        @Query("""
            SELECT u.personalObjective, COUNT(u)
            FROM User u
            WHERE u.id IN :userIds AND u.personalObjective IS NOT NULL
            GROUP BY u.personalObjective
            """)
        List<Object[]> countPersonalObjectivesByUserIds(@Param("userIds") List<Long> userIds);
}
