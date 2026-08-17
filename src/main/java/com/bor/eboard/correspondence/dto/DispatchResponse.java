package com.bor.eboard.correspondence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchResponse {

    private UUID id;
    private UUID letterId;
    private String letterNumber;
    private String letterSubject;
    private UUID fileId;
    private String fileNumber;
    private String dispatchNumber;
    private LocalDate dispatchDate;
    private String dispatchMode;
    private String recipientName;
    private String recipientDepartment;
    private String recipientAddress;
    private String trackingNumber;
    private String status;
    private String remarks;
    private LocalDateTime createdAt;
}
