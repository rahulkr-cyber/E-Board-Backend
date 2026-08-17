package com.bor.eboard.charge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class CreatePostingRequest {
    @NotNull
    private UUID userId;

    @NotBlank
    @Size(max = 50)
    private String postingType;

    @NotNull
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    private UUID departmentId;
    private UUID sectionId;
    private UUID designationId;

    @Size(max = 250)
    private String officeName;
    @Size(max = 150)
    private String seatName;
    private UUID reportingOfficerId;
    private UUID controllingOfficerId;
    @Size(max = 250)
    private String location;
    @Size(max = 150)
    private String district;
    @Size(max = 50)
    private String transferType;
    private String transferReason;

    @Size(max = 100)
    private String postingOrderNumber;
    private LocalDate postingOrderDate;
    @Size(max = 100)
    private String governmentOrderNumber;
    private LocalDate governmentOrderDate;

    private Boolean permanentCharge;
    private Boolean additionalCharge;
    private UUID currentChargeHolderId;
    private UUID previousChargeHolderId;
    private LocalDate chargeStartDate;
    private LocalDate chargeEndDate;
    @Size(max = 30)
    private String chargeStatus;

    private LocalDate joiningDate;
    private LocalTime joiningTime;
    @Size(max = 100)
    private String joiningOrder;
    private String joiningRemarks;
    private LocalDate relievingDate;
    private LocalTime relievingTime;
    @Size(max = 100)
    private String relievingOrder;
    private String relievingRemarks;
    private String remarks;
}
