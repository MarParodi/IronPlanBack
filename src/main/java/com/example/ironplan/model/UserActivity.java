package com.example.ironplan.model;
 
import com.example.ironplan.model.MetricType;
import jakarta.persistence.*;
import lombok.*;
 
import java.time.LocalDate;
import java.time.LocalDateTime;
 
@Entity
@Table(
    name = "user_activities",
    indexes = {
        @Index(name = "idx_activity_user_date",  columnList = "user_id, activity_date"),
        @Index(name = "idx_activity_metric",      columnList = "metric_type, activity_date")
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivity {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
 
    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    private MetricType metricType;
 
    @Column(name = "metric_value", nullable = false)
    @Builder.Default
    private Double metricValue = 0.0;
 
    /**
     * ID del workout_session que generó esta actividad.
     * Permite trazabilidad y evitar duplicados.
     */
    @Column(name = "source_id")
    private Long sourceId;
 
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
 
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
 