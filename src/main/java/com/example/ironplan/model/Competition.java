package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "competitions")
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competition {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;
	 
	    @Column(nullable = false, length = 200)
	    private String name;
	 
	    @Enumerated(EnumType.STRING)
	    @Column(name = "competition_type", nullable = false)
	    private CompetitionType competitionType;
	 
	    @Enumerated(EnumType.STRING)
	    @Column(name = "scope_level", nullable = false)
	    private ScopeLevel scopeLevel;
	 
	    // Referencia al nodo organizacional anfitrión (Carrera, Facultad o Empresa)
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "scope_reference_id", nullable = false)
	    private OrganizationalGroup scopeReference;
	 
	    @Enumerated(EnumType.STRING)
	    @Column(name = "metric_type", nullable = false)
	    private MetricType metricType;
	 
	    @Column(name = "start_date", nullable = false)
	    private LocalDate startDate;
	 
	    @Column(name = "end_date")
	    private LocalDate endDate; // NULL = ranking permanente
	 
	    @Enumerated(EnumType.STRING)
	    @Column(nullable = false)
	    @Builder.Default
	    private CompetitionStatus status = CompetitionStatus.DRAFT;
	 
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "created_by_user_id")
	    private User createdBy;
	 
	    @Column(name = "created_at", nullable = false, updatable = false)
	    private LocalDateTime createdAt;
	 
	    @OneToMany(mappedBy = "competition", cascade = CascadeType.ALL, orphanRemoval = true)
	    @Builder.Default
	    private List<CompetitionParticipant> participants = new ArrayList<>();
	 
	    @PrePersist
	    protected void onCreate() {
	        this.createdAt = LocalDateTime.now();
	    }
	 
	    // -------------------------------------------------------
	    // Helpers
	    // -------------------------------------------------------
	 
	    public boolean isExpired() {
	        return endDate != null && LocalDate.now().isAfter(endDate);
	    }
	 
	    public void activate() {
	        this.status = CompetitionStatus.ACTIVE;
	    }
	 
	    public void finish() {
	        this.status = CompetitionStatus.FINISHED;
	    }

}
