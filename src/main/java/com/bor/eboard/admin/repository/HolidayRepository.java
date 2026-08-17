package com.bor.eboard.admin.repository;

import com.bor.eboard.admin.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, UUID> {

    Optional<Holiday> findByIdAndDeletedFalse(UUID id);

    boolean existsByHolidayDateAndDeletedFalse(LocalDate holidayDate);

    List<Holiday> findByDeletedFalseOrderByHolidayDateAsc();

    List<Holiday> findByHolidayDateBetweenAndDeletedFalseOrderByHolidayDateAsc(
            LocalDate from, LocalDate to);
}
