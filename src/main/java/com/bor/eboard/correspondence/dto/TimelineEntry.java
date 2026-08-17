package com.bor.eboard.correspondence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single event in a file's timeline (04_API_SPEC.md 7.7).
 * Aggregated from letters, followups, reminders and dispatches so far;
 * workflow movements will be folded in during Phase 5.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEntry {

    private LocalDateTime timestamp;
    private String eventType;   // LETTER | FOLLOWUP | REMINDER | DISPATCH | FILE
    private String title;
    private String detail;
    private UUID referenceId;
    private String actorName;
}
