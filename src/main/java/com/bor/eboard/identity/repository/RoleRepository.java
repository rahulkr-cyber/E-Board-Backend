package com.bor.eboard.identity.repository;

import com.bor.eboard.identity.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByIdAndDeletedFalse(UUID id);

    Optional<Role> findByCodeAndDeletedFalse(String code);

    boolean existsByCodeAndDeletedFalse(String code);

    List<Role> findByDeletedFalseOrderByNameAsc();

    List<Role> findByIdInAndDeletedFalse(List<UUID> ids);
}
