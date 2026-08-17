package com.bor.eboard.charge.repository;

import com.bor.eboard.charge.entity.TransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferHistoryRepository extends JpaRepository<TransferHistory, UUID> {

    List<TransferHistory> findByUserIdOrderByTransferDateDesc(UUID userId);

    Optional<TransferHistory> findFirstByUserIdAndDeletedFalseOrderByTransferDateDesc(UUID userId);

    /** Soft-delete-aware lookup, needed to attach an order document. */
    Optional<TransferHistory> findByIdAndDeletedFalse(UUID id);
}
