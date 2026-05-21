package com.example.ironplan.model;

import com.example.ironplan.model.GroupType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(
    name = "organizational_groups",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_org_group_code",        columnNames = "code"),
        @UniqueConstraint(name = "UK_org_group_parent_name", columnNames = {"parent_id", "name"})
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationalGroup {
	
		@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;
	 
	    @Column(nullable = false, length = 200)
	    private String name;
	 
	    @Enumerated(EnumType.STRING)
	    @Column(name = "group_type", nullable = false)
	    private GroupType groupType;
	 
	    // Autorreferencia: grupo padre
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "parent_id")
	    private OrganizationalGroup parent;
	 
	    // Grupos hijos (solo para navegación)
	    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	    @Builder.Default
	    private List<OrganizationalGroup> children = new ArrayList<>();
	 
	    @Column(nullable = false, length = 64)
	    private String code;
	 
	    @Column(nullable = false)
	    @Builder.Default
	    private Boolean active = true;
	 
	    @Column(name = "created_at", nullable = false, updatable = false)
	    private LocalDateTime createdAt;
	    
	    @Enumerated(EnumType.STRING)
	    @Column(name = "organization_kind")
	    private OrganizationKind organizationKind; // null en nodos no-raíz
	 
	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "created_by_user_id")
	    private User createdBy;
	 
	    @PrePersist
	    protected void onCreate() {
	        this.createdAt = LocalDateTime.now();
	    }
	 
	    // -------------------------------------------------------
	    // Helpers de jerarquía
	    // -------------------------------------------------------
	 
	    /** Sube por parent_id hasta encontrar el ancestro del tipo indicado. */
	    public OrganizationalGroup findAncestorOfType(GroupType type) {
	        OrganizationalGroup current = this;
	        while (current != null) {
	            if (current.getGroupType() == type) return current;
	            current = current.getParent();
	        }
	        return null;
	    }
	 
	    /** Verifica si este grupo es descendiente del nodo con el id dado. */
	    public boolean isDescendantOf(Long ancestorId) {
	        OrganizationalGroup current = this.parent;
	        while (current != null) {
	            if (current.getId().equals(ancestorId)) return true;
	            current = current.getParent();
	        }
	        return false;
	    }

}
