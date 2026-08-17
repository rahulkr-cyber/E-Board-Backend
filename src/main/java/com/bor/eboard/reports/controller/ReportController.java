package com.bor.eboard.reports.controller;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.reports.dto.ReportData;
import com.bor.eboard.reports.dto.ReportRequest;
import com.bor.eboard.reports.export.ExcelReportExporter;
import com.bor.eboard.reports.export.PdfReportExporter;
import com.bor.eboard.reports.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Report APIs (04_API_SPEC.md section 13). One data endpoint and one export
 * endpoint serve every report code; the code selects the report and the
 * format selects PDF or Excel. Exports are recorded in the audit trail
 * (10_SECURITY.md — downloads/exports are auditable events).
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Operational reports with PDF/Excel export")
public class ReportController {

    private final ReportService reportService;
    private final PdfReportExporter pdfExporter;
    private final ExcelReportExporter excelExporter;
    private final AuditService auditService;

    @Operation(summary = "Get report data as JSON")
    @GetMapping("/{reportCode}")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ApiResponse<ReportData> data(
            @PathVariable String reportCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID officerId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID priorityId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean overdueOnly,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        ReportRequest request = new ReportRequest(fromDate, toDate, sectionId, officerId,
                categoryId, priorityId, status, overdueOnly, month, year);
        return ApiResponse.success(reportService.build(reportCode, request));
    }

    @Operation(summary = "Export a report as PDF or Excel")
    @GetMapping("/{reportCode}/export")
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<ByteArrayResource> export(
            @PathVariable String reportCode,
            @RequestParam(defaultValue = "PDF") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID officerId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID priorityId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean overdueOnly,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        ReportRequest request = new ReportRequest(fromDate, toDate, sectionId, officerId,
                categoryId, priorityId, status, overdueOnly, month, year);
        ReportData report = reportService.build(reportCode, request);

        boolean excel = "EXCEL".equalsIgnoreCase(format) || "XLSX".equalsIgnoreCase(format);
        byte[] bytes = excel ? excelExporter.export(report) : pdfExporter.export(report);
        String extension = excel ? "xlsx" : "pdf";
        MediaType mediaType = excel
                ? MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.APPLICATION_PDF;
        String filename = reportCode + "-" + LocalDate.now() + "." + extension;

        auditService.record("REPORTS", "REPORT", null, "EXPORT",
                null, "code=" + reportCode + ", format=" + (excel ? "EXCEL" : "PDF")
                        + ", by=" + SecurityUtils.getCurrentUserId().map(UUID::toString).orElse("?"));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }
}
