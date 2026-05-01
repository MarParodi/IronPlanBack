package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.*;
 
import java.time.LocalDate;
 
@Entity
@Table(name = "organizational_invitations")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationalInvitation {

	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;
	 
	    @Column(nullable = false, unique = true, length = 64)
	    private String code;
	 
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "organizational_group_id", nullable = false)
	    private OrganizationalGroup group;
	 
	    @Column(name = "expires_at")
	    private LocalDate expiresAt;   // NULL = sin caducidad
	 
	    @Column(name = "max_uses")
	    private Integer maxUses;           // NULL = ilimitado
	 
	    @Column(name = "uses_count", nullable = false)
	    @Builder.Default
	    private Integer usesCount = 0;
	 
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "created_by_user_id")
	    private User createdBy;
	 
	    @Column(nullable = false)
	    @Builder.Default
	    private Boolean active = true;
	    
	    
	    public boolean isValid() {
	        if (!active) return false;
	        if (expiresAt != null && LocalDate.now().isAfter(expiresAt)) return false;
	        if (maxUses != null && usesCount >= maxUses) return false;
	        return true;
	    }
	 
	    public void registerUse() {
	        this.usesCount++;
	    }
}
