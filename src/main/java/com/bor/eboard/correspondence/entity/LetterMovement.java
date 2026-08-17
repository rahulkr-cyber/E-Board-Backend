package com.bor.eboard.correspondence.entity;

import com.bor.eboard.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable movement trail for letters (BCR-03 Part 16), mirroring the
 * workflow movement record for files. A database trigger blocks any UPDATE
 * or DELETE on this table.
 */
@Entity
@Table(name = "correspondence_letter_movements")
@Getter
@Setter
@NoArgsConstructor
public class LetterMovement extends BaseEntity {

    @Column(name = "letter_id", nullable = false)
    private UUID letterId;

    /** MARK | RETURN | CLOSE | REOPEN */
    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "from_user_id")
    private UUID fromUserId;

    @Column(name = "from_section_id")
    private UUID fromSectionId;

    @Column(name = "to_user_id")
    private UUID toUserId;

    @Column(name = "to_section_id")
    private UUID toSectionId;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "action_at", nullable = false)
    private LocalDateTime actionAt;

    @Column(name = "action_by", nullable = false)
    private UUID actionBy;
}
