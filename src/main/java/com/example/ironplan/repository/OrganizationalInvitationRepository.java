package com.example.ironplan.repository;
 
import com.example.ironplan.model.OrganizationalInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.List;
import java.util.Optional;
 
public interface OrganizationalInvitationRepository extends JpaRepository<OrganizationalInvitation, Long> {
 
    Optional<OrganizationalInvitation> findByCode(String code);
 
    List<OrganizationalInvitation> findByGroupId(Long groupId);
 
    List<OrganizationalInvitation> findByActive(Boolean active);
 
    List<OrganizationalInvitation> findByGroupIdAndActive(Long groupId, Boolean active);
 
    boolean existsByCode(String code);
}