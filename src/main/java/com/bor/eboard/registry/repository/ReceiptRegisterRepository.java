package com.bor.eboard.registry.repository;

import com.bor.eboard.registry.entity.ReceiptRegisterEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptRegisterRepository extends JpaRepository<ReceiptRegisterEntry, UUID> {

    Optional<ReceiptRegisterEntry> findByDiaryEntryId(UUID diaryEntryId);
}
