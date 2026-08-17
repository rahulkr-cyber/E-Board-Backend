package com.bor.eboard.correspondence.repository;

import com.bor.eboard.correspondence.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    Optional<Reminder> findByIdAndDeletedFalse(UUID id);

    List<Reminder> findByFileIdAndDeletedFalseOrderByReminderDateDesc(UUID fileId);
}
