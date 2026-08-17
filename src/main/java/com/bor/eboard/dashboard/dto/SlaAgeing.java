package com.bor.eboard.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pendency ageing aligned to the 3/7/15/30-day SLA thresholds
 * (05_UI_UX.md, 06_BUSINESS_RULES.md section 10): Reminder, Warning,
 * Escalation, Highest Escalation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaAgeing {

    private long onTrack;          // not overdue
    private long reminder;         // >= 3 days overdue
    private long warning;          // >= 7 days overdue
    private long escalation;       // >= 15 days overdue
    private long highestEscalation; // >= 30 days overdue
}
