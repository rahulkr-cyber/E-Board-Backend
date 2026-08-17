package com.bor.eboard.identity.repository;

import com.bor.eboard.identity.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameAndDeletedFalse(String username);

    Optional<User> findByIdAndDeletedFalse(UUID id);

    boolean existsByUsernameAndDeletedFalse(String username);

    boolean existsByEmployeeCodeAndDeletedFalse(String employeeCode);

    List<User> findByIdInAndDeletedFalse(List<UUID> ids);

    List<User> findBySectionIdAndDeletedFalseAndStatus(UUID sectionId, String status);

    @Query("""
    	    SELECT u
    	    FROM User u
    	    WHERE u.deleted = FALSE
    	      AND (
    	            :query IS NULL
    	            OR LOWER(u.username) LIKE :query
    	            OR LOWER(u.fullName) LIKE :query
    	            OR LOWER(u.employeeCode) LIKE :query
    	      )
    	      AND (
    	            :sectionId IS NULL
    	            OR u.sectionId = :sectionId
    	      )
    	      AND (
    	            :departmentId IS NULL
    	            OR u.departmentId = :departmentId
    	      )
    	      AND (
    	            :status IS NULL
    	            OR u.status = :status
    	      )
    	    """)
    	Page<User> search(
    	        @Param("query") String query,
    	        @Param("sectionId") UUID sectionId,
    	        @Param("departmentId") UUID departmentId,
    	        @Param("status") String status,
    	        Pageable pageable);

    /**
     * Active users holding the given role, optionally restricted to a section.
     * Backs the assignment-rule resolver (BCR-03 Part 5): the workflow picks
     * the next role, this returns the users actually eligible for it.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.deleted = FALSE
              AND u.status = 'ACTIVE'
              AND (:sectionId IS NULL OR u.sectionId = :sectionId)
              AND EXISTS (
                  SELECT 1 FROM UserRole ur
                  WHERE ur.userId = u.id
                    AND ur.roleId = :roleId
                    AND ur.active = TRUE
              )
            ORDER BY u.fullName ASC
            """)
    List<User> findActiveByRoleAndOptionalSection(@Param("roleId") UUID roleId,
                                                  @Param("sectionId") UUID sectionId);
}
