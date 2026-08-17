package com.bor.eboard.assignment.controller;

import com.bor.eboard.assignment.dto.AssignmentRuleRequest;
import com.bor.eboard.assignment.dto.AssignmentRuleResponse;
import com.bor.eboard.assignment.dto.MarkTargetResponse;
import com.bor.eboard.assignment.service.AssignmentService;
import com.bor.eboard.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assignment")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    // ---------- Mark dialog support (any user who may mark) ----------

    /**
     * Roles the caller may mark to; and, when {@code toRoleId} is supplied,
     * the users eligible under the active rules.
     */
    @GetMapping("/mark-targets")
    @PreAuthorize("hasAnyAuthority('FILE_MARK', 'LETTER_MARK')")
    public ResponseEntity<ApiResponse<MarkTargetResponse>> markTargets(
            @RequestParam(required = false) UUID toRoleId,
            @RequestParam(required = false) UUID toSectionId) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.markTargets(toRoleId, toSectionId)));
    }

    // ---------- Rule administration ----------

    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('ASSIGNMENT_RULE_MANAGE')")
    public ResponseEntity<ApiResponse<List<AssignmentRuleResponse>>> listRules() {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.listRules()));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasAuthority('ASSIGNMENT_RULE_MANAGE')")
    public ResponseEntity<ApiResponse<AssignmentRuleResponse>> createRule(
            @Valid @RequestBody AssignmentRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Assignment rule created", assignmentService.createRule(request)));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_RULE_MANAGE')")
    public ResponseEntity<ApiResponse<AssignmentRuleResponse>> updateRule(
            @PathVariable UUID id, @Valid @RequestBody AssignmentRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Assignment rule updated", assignmentService.updateRule(id, request)));
    }

    @PatchMapping("/rules/{id}/status")
    @PreAuthorize("hasAuthority('ASSIGNMENT_RULE_MANAGE')")
    public ResponseEntity<ApiResponse<AssignmentRuleResponse>> setStatus(
            @PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.setRuleStatus(id, active)));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_RULE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable UUID id) {
        assignmentService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.successMessage("Assignment rule deleted"));
    }
}
