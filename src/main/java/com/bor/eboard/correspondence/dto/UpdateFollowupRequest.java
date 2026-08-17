package com.bor.eboard.correspondence.dto;

import lombok.Data;

import java.time.LocalDate;

/** Record a reply, or revise the dates, on an existing follow-up. */
@Data
public class UpdateFollowupRequest {
	  private String status;

	    private Boolean replyReceived;

	    private LocalDate replyReceivedDate;

	    private LocalDate dueDate;

	    private LocalDate reminderDate;

	    // Required by your service
	    private LocalDate nextFollowupDate;

	    private String remarks;
}
