package com.bor.eboard.charge.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class CreateJoiningRelievingRequest {

    @NotNull
    private UUID userId;

    @NotNull
    @Size(max = 30)
    private String eventType; // JOINING | RELIEVING

    private UUID departmentId;
    private UUID sectionId;
    private UUID designationId;

    @NotNull
    private LocalDate eventDate;
    private LocalTime eventTime;

    @Size(max = 100)
    private String orderNumber;
    private LocalDate orderDate;
    @Size(max = 100)
    private String governmentOrderNumber;
    private LocalDate governmentOrderDate;
    private UUID attachmentId;
    private String remarks;
}
