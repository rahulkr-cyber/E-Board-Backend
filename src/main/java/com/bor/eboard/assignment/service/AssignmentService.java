package com.bor.eboard.assignment.service;

import com.bor.eboard.assignment.dto.AssignmentRuleRequest;
import com.bor.eboard.assignment.dto.AssignmentRuleResponse;
import com.bor.eboard.assignment.dto.MarkTargetResponse;

import java.util.List;
import java.util.UUID;

public interface AssignmentService {

    // --- rule administration ---
    List<AssignmentRuleResponse> listRules();

    AssignmentRuleResponse createRule(AssignmentRuleRequest request);

    AssignmentRuleResponse updateRule(UUID id, AssignmentRuleRequest request);

    AssignmentRuleResponse setRuleStatus(UUID id, boolean active);

    void deleteRule(UUID id);

    // --- resolution for the Mark dialog ---

    /** Roles the current user may mark to, per the active rules. */
    MarkTargetResponse markTargets(UUID toRoleId, UUID toSectionId);
}
