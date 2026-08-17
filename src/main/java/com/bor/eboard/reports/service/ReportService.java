package com.bor.eboard.reports.service;

import com.bor.eboard.reports.dto.ReportData;
import com.bor.eboard.reports.dto.ReportRequest;

/**
 * Builds render-agnostic {@link ReportData} for a given report code
 * (11_REPORTS.md). Supported codes: diary-register, dispatch-register,
 * pending, disposed, monthly-pocket.
 */
public interface ReportService {

    ReportData build(String reportCode, ReportRequest request);
}
