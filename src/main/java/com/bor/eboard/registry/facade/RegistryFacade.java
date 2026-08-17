package com.bor.eboard.registry.facade;

import java.util.UUID;

/**
 * Cross-module entry point exposing registry aggregates to the Dashboard
 * module (02_ARCHITECTURE.md section 25).
 */
public interface RegistryFacade {

    /** Diary entries received today, optionally scoped to a section. */
    long diariesReceivedToday(UUID sectionId);

    /** Diary entries received in an inclusive date range. */
    long diariesReceivedBetween(java.time.LocalDate fromDate,
                                java.time.LocalDate toDate,
                                UUID sectionId);

    long diariesReceivedBetweenSections(
            java.time.LocalDate fromDate,
            java.time.LocalDate toDate,
            java.util.Collection<UUID> sectionIds);

    /** Diary entries currently in a given status, optionally section-scoped. */
    long diariesByStatus(String status, UUID sectionId);

    /** A diary row for the Diary Register report. */
    record DiaryReportRow(
            String diaryNumber,
            java.time.LocalDateTime receivedDate,
            String senderName,
            String senderDepartment,
            String letterNumber,
            java.time.LocalDate letterDate,
            String subject,
            String categoryName,
            String priorityName,
            String sectionName,
            String status) {
    }

    java.util.List<DiaryReportRow> diaryRegisterReport(java.time.LocalDate fromDate,
                                                       java.time.LocalDate toDate,
                                                       UUID sectionId, String status);

    /** Business diary numbers for dashboard/report references, resolved in one batch. */
    java.util.Map<UUID, String> diaryNumbers(java.util.Collection<UUID> diaryEntryIds);
}
