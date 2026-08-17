package com.bor.eboard.charge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Consolidated posting history for a user: their transfers, joining/relieving
 * events, and charge assignments (both held and granted).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostingResponse {

    private UUID userId;
    private String userName;
    private EmployeeHeaderResponse employee;
    private List<PostingTimelineEntryResponse> timeline;
    private List<TransferResponse> transfers;
    private List<JoiningRelievingResponse> joiningRelievingEvents;
    private List<ChargeResponse> chargesHeld;    // charge granted TO this user
    private List<ChargeResponse> chargesGranted; // charge granted BY this user
}
