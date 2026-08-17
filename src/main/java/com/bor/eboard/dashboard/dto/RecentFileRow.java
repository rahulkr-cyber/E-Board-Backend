package com.bor.eboard.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentFileRow {

    private UUID fileId;
    private String fileNumber;
    private String subject;
    private String currentStatus;
    private LocalDate openedDate;
}
