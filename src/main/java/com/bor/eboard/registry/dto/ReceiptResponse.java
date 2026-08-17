package com.bor.eboard.registry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptResponse {

    private UUID id;
    private String receiptNumber;
    private String receivedFrom;
    private UUID receivedBy;
    private String receivedByName;
    private LocalDateTime receivedAt;
    private String remarks;
}
