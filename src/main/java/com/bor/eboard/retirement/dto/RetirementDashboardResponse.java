package com.bor.eboard.retirement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetirementDashboardResponse {
    private String scope;
    private UUID scopeId;
    private String scopeLabel;

    private long within180Days;
    private long within90Days;
    private long within30Days;
    private long retiredToday;
    private long retiringThisYear;
    private long alreadyRetired;

    /** Backward-compatible JSON fields for the previous embedded dashboard API. */
    private long withinSixMonths;
    private long withinThreeMonths;
    private long withinOneMonth;
    private long today;

    private List<RetirementEmployeeRow> employees;
}
