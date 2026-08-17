package com.bor.eboard.charge.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateChargeRequest {

    @NotNull
    private UUID fromUserId;

    @NotNull
    private UUID toUserId;

    @NotNull
    private String chargeType; // TEMPORARY | ADDITIONAL | FULL_CHARGE

    private UUID departmentId;
    private UUID sectionId;

    @NotNull
    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;
    private String orderNumber;
    private LocalDate orderDate;
    private UUID approvedBy;
    private UUID attachmentId;
    private String reason;
}
