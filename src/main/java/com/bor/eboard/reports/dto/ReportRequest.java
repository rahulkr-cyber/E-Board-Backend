package com.bor.eboard.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Union of filters across the built-in reports. Each report reads only the
 * filters relevant to it; unrelated fields are ignored.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    private LocalDate fromDate;
    private LocalDate toDate;
    private UUID sectionId;
    private UUID officerId;
    private UUID categoryId;
    private UUID priorityId;
    private String status;
    private boolean overdueOnly;
    private Integer month;
    private Integer year;
}
