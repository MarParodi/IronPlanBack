package com.example.ironplan.repository;
 
import com.example.ironplan.model.UserActivity;
import com.example.ironplan.model.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import java.time.LocalDate;
import java.util.List;
 
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
 
    boolean existsBySourceIdAndMetricType(Long sourceId, MetricType metricType);
 
    // Score grupal: suma de actividad de todos los miembros de un grupo en un período
    @Query("""
        SELECT COALESCE(SUM(a.metricValue), 0.0)
        FROM UserActivity a
        JOIN a.user u
        WHERE u.primaryOrganizationalGroup.id = :groupId
        AND a.metricType = :metricType
        AND a.activityDate BETWEEN :startDate AND :endDate
    """)
    Double sumGroupScore(
        @Param("groupId")    Long groupId,
        @Param("metricType") MetricType metricType,
        @Param("startDate")  LocalDate startDate,
        @Param("endDate")    LocalDate endDate
    );
 
    // Ranking interno: actividad individual de cada miembro de un grupo
    @Query("""
        SELECT u.id, u.firstName, u.lastName, u.username, u.profilePictureUrl,
               COALESCE(SUM(a.metricValue), 0.0) AS score
        FROM User u
        LEFT JOIN UserActivity a ON a.user.id = u.id
            AND a.metricType = :metricType
            AND a.activityDate BETWEEN :startDate AND :endDate
        WHERE u.primaryOrganizationalGroup.id = :groupId
        GROUP BY u.id, u.firstName, u.lastName, u.username, u.profilePictureUrl
        ORDER BY score DESC
    """)
    List<Object[]> findInternalRanking(
        @Param("groupId")    Long groupId,
        @Param("metricType") MetricType metricType,
        @Param("startDate")  LocalDate startDate,
        @Param("endDate")    LocalDate endDate
    );
    
    
    @Query("""
    	    SELECT COALESCE(SUM(a.metricValue), 0.0)
    	    FROM UserActivity a
    	    WHERE a.user.id = :userId
    	    AND a.metricType = :metricType
    	    AND a.activityDate BETWEEN :startDate AND :endDate
    	""")
    	Double sumUserScore(
    	    @Param("userId")     Long userId,
    	    @Param("metricType") MetricType metricType,
    	    @Param("startDate")  LocalDate startDate,
    	    @Param("endDate")    LocalDate endDate
    	);
}