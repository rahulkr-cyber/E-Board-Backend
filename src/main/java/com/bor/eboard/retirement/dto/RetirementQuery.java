package com.bor.eboard.retirement.dto;

import com.bor.eboard.dashboard.dto.DashboardScopeType;

import java.util.UUID;

public record RetirementQuery(
        DashboardScopeType scopeType,
        UUID scopeId,
        UUID departmentId,
        UUID sectionId,
        UUID designationId,
        String office,
        Integer retirementMonth,
        Integer retirementYear,
        RetirementPeriod retirementPeriod,
        String search,
        RetirementSort sortBy,
        String district,
        String employeeStatus,
        String employeeName,
        String employeeCode) {

    public DashboardScopeType effectiveScopeType() {
        return scopeType == null ? DashboardScopeType.SELF : scopeType;
    }

    public RetirementPeriod effectivePeriod() {
        return retirementPeriod == null ? RetirementPeriod.WITHIN_180_DAYS : retirementPeriod;
    }

    public RetirementSort effectiveSort() {
        return sortBy == null ? RetirementSort.NEAREST_RETIREMENT : sortBy;
    }
}
