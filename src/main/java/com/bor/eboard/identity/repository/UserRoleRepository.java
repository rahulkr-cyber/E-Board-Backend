package com.bor.eboard.identity.repository;

import com.bor.eboard.identity.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUserIdAndActiveTrue(UUID userId);

    List<UserRole> findByRoleIdAndActiveTrue(UUID roleId);

    List<UserRole> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
