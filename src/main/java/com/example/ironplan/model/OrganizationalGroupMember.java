package com.example.ironplan.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "organizational_group_members",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_org_group_member_user_group",
        columnNames = {"user_id", "organizational_group_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationalGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizational_group_id", nullable = false)
    private OrganizationalGroup group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupMembershipRole role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
    }
}
