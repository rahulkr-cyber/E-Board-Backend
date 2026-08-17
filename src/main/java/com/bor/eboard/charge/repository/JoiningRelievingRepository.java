package com.bor.eboard.charge.repository;

import com.bor.eboard.charge.entity.JoiningRelieving;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JoiningRelievingRepository extends JpaRepository<JoiningRelieving, UUID> {

    List<JoiningRelieving> findByUserIdOrderByEventDateDesc(UUID userId);

    /** Soft-delete-aware lookup, needed to attach an order document. */
    Optional<JoiningRelieving> findByIdAndDeletedFalse(UUID id);
}
