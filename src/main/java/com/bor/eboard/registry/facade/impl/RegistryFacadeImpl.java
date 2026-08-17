package com.bor.eboard.registry.facade.impl;

import com.bor.eboard.registry.facade.RegistryFacade;
import com.bor.eboard.registry.repository.DiaryEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RegistryFacadeImpl implements RegistryFacade {

    private final DiaryEntryRepository diaryEntryRepository;
    private final com.bor.eboard.admin.facade.MasterDataFacade masterDataFacade;
    private final com.bor.eboard.identity.facade.IdentityFacade identityFacade;

    @Override
    @Transactional(readOnly = true)
    public long diariesReceivedToday(UUID sectionId) {
        LocalDate today = LocalDate.now();
        return diariesReceivedBetween(today, today, sectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public long diariesReceivedBetween(LocalDate fromDate, LocalDate toDate,
                                       UUID sectionId) {
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        return diaryEntryRepository.countReceivedBetween(from, to, sectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public long diariesReceivedBetweenSections(
            LocalDate fromDate, LocalDate toDate,
            java.util.Collection<UUID> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) {
            return 0;
        }
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        return diaryEntryRepository.countReceivedBetweenSections(
                from, to, sectionIds);
    }

    @Override
    @Transactional(readOnly = true)
    public long diariesByStatus(String status, UUID sectionId) {
        return diaryEntryRepository.countByStatusAndScope(status, sectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<DiaryReportRow> diaryRegisterReport(LocalDate fromDate,
                                                              LocalDate toDate,
                                                              UUID sectionId, String status) {
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toDate.atTime(java.time.LocalTime.MAX) : null;
        String statusFilter = (status == null || status.isBlank()) ? null : status.trim();
        java.util.Map<UUID, String> categories = masterDataFacade.categoryNames();
        java.util.Map<UUID, String> priorities = masterDataFacade.priorityNames();
        java.util.Map<UUID, String> sections = identityFacade.sectionNames();
        return diaryEntryRepository.diaryRegisterFiltered(from, to, sectionId, statusFilter).stream()
                .map(d -> new DiaryReportRow(
                        d.getDiaryNumber(), d.getReceivedDate(),
                        d.getSenderName(), d.getSenderDepartment(),
                        d.getOriginalLetterNumber(), d.getLetterDate(),
                        d.getSubject(),
                        categories.get(d.getCategoryId()),
                        priorities.get(d.getPriorityId()),
                        sections.get(d.getInitialSectionId()),
                        d.getStatus()))
                .toList();
    }
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.Map<UUID, String> diaryNumbers(java.util.Collection<UUID> diaryEntryIds) {
        if (diaryEntryIds == null || diaryEntryIds.isEmpty()) {
            return java.util.Map.of();
        }
        return diaryEntryRepository.findAllById(diaryEntryIds).stream()
                .filter(entry -> !Boolean.TRUE.equals(entry.getDeleted()))
                .collect(java.util.stream.Collectors.toMap(
                        com.bor.eboard.registry.entity.DiaryEntry::getId,
                        com.bor.eboard.registry.entity.DiaryEntry::getDiaryNumber));
    }

}
