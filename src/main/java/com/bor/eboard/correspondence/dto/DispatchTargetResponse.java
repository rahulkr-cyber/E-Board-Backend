package com.bor.eboard.correspondence.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * BCR-03 Part 9: a searchable dispatch target. The clerk never types a UUID —
 * they search by diary/file/letter number or subject and pick a row; the
 * subject, section, owner and category then auto-populate.
 */
@Data
@Builder
public class DispatchTargetResponse {

    /** FILE or LETTER */
    private String targetType;
    private UUID targetId;

    /** File number, letter number, or diary number — whatever identifies it. */
    private String referenceNumber;
    private String diaryNumber;
    private String subject;

    // Auto-populated context
    private UUID sectionId;
    private String sectionName;
    private UUID currentOwnerId;
    private String currentOwnerName;
    private UUID categoryId;
    private String categoryName;
}
