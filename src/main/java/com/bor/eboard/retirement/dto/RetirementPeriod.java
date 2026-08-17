package com.bor.eboard.retirement.dto;

public enum RetirementPeriod {
    ALL,
    WITHIN_180_DAYS,
    WITHIN_90_DAYS,
    WITHIN_30_DAYS,
    RETIRED_TODAY,
    ALREADY_RETIRED,

    /** Backward-compatible aliases retained for existing API clients. */
    WITHIN_6_MONTHS,
    WITHIN_3_MONTHS,
    WITHIN_1_MONTH,
    TODAY
}
