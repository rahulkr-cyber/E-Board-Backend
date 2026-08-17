package com.bor.eboard.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * A fully-resolved, render-agnostic report. Both the JSON response and the
 * PDF/Excel exporters consume this same structure, so a report is defined
 * once and rendered three ways.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportData {

    private String reportCode;
    private String title;
    private List<ReportColumn> columns;
    private List<Map<String, Object>> rows;
    private List<String> filterSummary;   // human-readable "Filter: value" lines
    private Map<String, Object> totals;   // optional column totals keyed by column key
    private String generatedByName;
    private LocalDateTime generatedAt;
}
