package com.bor.eboard.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A report column: a stable key, a display header, and an optional width hint. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportColumn {

    private String key;
    private String header;
    private int widthHint; // relative width for PDF/Excel sizing; 0 = default

    public ReportColumn(String key, String header) {
        this(key, header, 0);
    }
}
