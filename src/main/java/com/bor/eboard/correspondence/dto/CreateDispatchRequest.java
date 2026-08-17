package com.bor.eboard.correspondence.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Create dispatch record for an outward letter
 * (03_DATABASE.md 8.6, dispatch number BOR/DISPATCH/{YEAR}/{SEQ}).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDispatchRequest {

    @NotNull(message = "Letter is required")
    private UUID letterId;

    /**
     * BCR-03 Part 9: a dispatch may be raised against a File instead of a
     * Letter. Exactly one of letterId / fileId must be supplied.
     */
    private UUID fileId;

    @NotNull(message = "Dispatch date is required")
    private LocalDate dispatchDate;

    @Pattern(regexp = "^$|POST|COURIER|HAND_DELIVERY|EMAIL|FAX|PORTAL|OTHER",
            message = "Invalid dispatch mode")
    private String dispatchMode;

    @Size(max = 200)
    private String recipientName;

    @Size(max = 200)
    private String recipientDepartment;

    private String recipientAddress;

    @Size(max = 150)
    private String trackingNumber;

    private String remarks;
}
