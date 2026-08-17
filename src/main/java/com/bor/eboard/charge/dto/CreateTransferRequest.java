package com.bor.eboard.charge.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateTransferRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID toDepartmentId;

    @NotNull
    private UUID toSectionId;

    private UUID toDesignationId;
    @Size(max = 50)
    private String postingType;
    @Size(max = 50)
    private String transferType;
    @Size(max = 250)
    private String toOfficeName;
    @Size(max = 150)
    private String seatName;
    private UUID reportingOfficerId;
    private UUID controllingOfficerId;
    @Size(max = 250)
    private String toLocation;
    @Size(max = 150)
    private String toDistrict;

    @NotNull
    private LocalDate transferDate;
    private LocalDate effectiveToDate;

    @Size(max = 100)
    private String orderNumber;
    private LocalDate orderDate;
    @Size(max = 100)
    private String governmentOrderNumber;
    private LocalDate governmentOrderDate;
    private UUID attachmentId;
    private String transferReason;
    private String remarks;
}
