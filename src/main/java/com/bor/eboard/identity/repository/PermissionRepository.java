package com.bor.eboard.identity.repository;

import com.bor.eboard.identity.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByIdAndDeletedFalse(UUID id);

    List<Permission> findByDeletedFalseOrderByModuleAscCodeAsc();

    List<Permission> findByIdInAndDeletedFalse(List<UUID> ids);
}
