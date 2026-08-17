package com.bor.eboard.assignment.facade;

import java.util.List;
import java.util.UUID;

/**
 * Read-only view of the assignment rules for other modules (correspondence,
 * workflow). Modules never touch the assignment repositories directly.
 */
public interface AssignmentFacade {

    /**
     * True when a user holding any of {@code fromRoleIds} is permitted by an
     * active rule to mark work to {@code toUserId} in {@code toSectionId}.
     *
     * @param fromSectionId the marking user's section, for same/cross-section checks
     */
    boolean canMark(List<UUID> fromRoleIds, UUID fromSectionId, UUID toUserId, UUID toSectionId);
}
