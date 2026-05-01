package com.example.ironplan.repository;
 
import com.example.ironplan.model.OrganizationalGroup;
import com.example.ironplan.model.GroupType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import java.util.List;
import java.util.Optional;
 
public interface OrganizationalGroupRepository extends JpaRepository<OrganizationalGroup, Long> {
 
    List<OrganizationalGroup> findByGroupType(GroupType groupType);
 
    List<OrganizationalGroup> findByActive(Boolean active);
 
    List<OrganizationalGroup> findByGroupTypeAndActive(GroupType groupType, Boolean active);
 
    List<OrganizationalGroup> findByParentId(Long parentId);
 
    Optional<OrganizationalGroup> findByCode(String code);
 
    boolean existsByCode(String code);
 
    boolean existsByNameAndParentId(String name, Long parentId);
 
    // Todos los grupos hoja (tipo GRUPO) activos
    List<OrganizationalGroup> findByGroupTypeAndActiveTrue(GroupType groupType);
 
    /*
    // Verifica si un grupo es descendiente de un ancestro dado
    @Query("""
        SELECT COUNT(g) > 0 FROM OrganizationalGroup g
        WHERE g.id = :groupId
        AND (
            g.parent.id = :ancestorId
            OR g.parent.parent.id = :ancestorId
            OR g.parent.parent.parent.id = :ancestorId
        )
    """)
    boolean isDescendantOf(@Param("groupId") Long groupId, @Param("ancestorId") Long ancestorId);
    
    
    */
    
    
    @Query("""
    	    SELECT COUNT(g) > 0 FROM OrganizationalGroup g
    	    WHERE g.id = :groupId AND (
    	        g.parent.id = :ancestorId
    	        OR g.parent.parent.id = :ancestorId
    	        OR g.parent.parent.parent.id = :ancestorId
    	    )
    	""")
    	boolean isDescendantOf(@Param("groupId") Long groupId, @Param("ancestorId") Long ancestorId);
 
    // Todos los grupos dentro de un scope (hijos directos e indirectos)
    @Query("""
        SELECT g FROM OrganizationalGroup g
        WHERE g.groupType = 'GRUPO' AND g.active = true
        AND (
            g.parent.id = :scopeId
            OR g.parent.parent.id = :scopeId
            OR g.parent.parent.parent.id = :scopeId
        )
    """)
    List<OrganizationalGroup> findLeafGroupsUnder(@Param("scopeId") Long scopeId);
}