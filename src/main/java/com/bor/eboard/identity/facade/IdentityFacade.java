package com.bor.eboard.identity.facade;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-module entry point into the Identity module
 * (02_ARCHITECTURE.md section 25: other modules must not touch
 * Identity repositories directly).
 */
public interface IdentityFacade {

    record SectionRef(UUID id, UUID departmentId, String name) {
    }

    record UserRef(UUID id, UUID sectionId, UUID departmentId, UUID designationId,
                   String fullName, boolean active) {
    }

    record EmployeeRef(UUID id, String employeeCode, String fullName, String email,
                       String mobile, String status, UUID departmentId, UUID sectionId,
                       UUID designationId, LocalDate dateOfBirth, LocalDate dateOfJoining,
                       UUID profileAttachmentId, String district) {
    }

    Optional<SectionRef> findSection(UUID sectionId);

    Optional<UserRef> findUser(UUID userId);

    Optional<EmployeeRef> findEmployee(UUID userId);

    List<EmployeeRef> employees();

    boolean departmentExists(UUID departmentId);

    boolean roleExists(UUID roleId);

    boolean designationExists(UUID designationId);

    Map<UUID, String> departmentNames();

    Map<UUID, String> sectionNames();

    /** Active departments for dashboard scope selection. */
    Map<UUID, String> activeDepartmentNames();

    /** Active sections, optionally restricted to one department. */
    List<SectionRef> activeSections(UUID departmentId);

    /** Active users for dashboard USER/OFFICER scope selection. */
    List<UserRef> searchActiveUsers(String query, UUID departmentId,
                                    UUID sectionId, int limit);

    Map<UUID, String> userNames();

    Map<UUID, String> roleNames();

    /** Designation id -> name, for posting/transfer history display. */
    Map<UUID, String> designationNames();

    /**
     * Apply a transfer to a user's substantive posting: updates current
     * department, section, and (if given) designation. Used by the Charge
     * module's TransferService (13_BUSINESS_RULES.md section 13 rule 2).
     */
    default void updateUserPosting(UUID userId, UUID departmentId, UUID sectionId, UUID designationId) {
        updateUserPosting(userId, departmentId, sectionId, designationId, null);
    }

    void updateUserPosting(UUID userId, UUID departmentId, UUID sectionId,
                           UUID designationId, String district);

    /** Set a user's status (e.g. INACTIVE on relieving). */
    void updateUserStatus(UUID userId, String status);

    /** Active role ids currently held by the user (effective-dated). */
    List<UUID> activeRoleIdsOf(UUID userId);

    /**
     * Resolve the concrete user a workflow step should be assigned to when
     * the step names a specific user, otherwise leave user null and let the
     * task target role/section. Returns the best single assignee (specific
     * user > first active user with the role in the section > null).
     */
    Optional<UUID> resolveStepAssignee(UUID specificUserId, UUID roleId,
                                       UUID designationId, UUID sectionId);

    /**
     * True when the given user satisfies a task assigned to the given
     * user/role/section triple (direct user match, role membership, or
     * section membership).
     */
    boolean userMatchesAssignment(UUID userId, UUID assignedUserId,
                                  UUID assignedRoleId, UUID assignedSectionId);

    // ---------------------------------------------------------------------
    // BCR-03 (Part 5): support for DB-driven assignment rules. Additive only.
    // ---------------------------------------------------------------------

    /**
     * Active users holding {@code roleId}, optionally restricted to
     * {@code sectionId} (pass {@code null} for any section). Used by the
     * assignment resolver to offer the actual users behind a target role.
     */
    List<UserRef> activeUsersByRole(UUID roleId, UUID sectionId);

    /** Role codes for the given role ids, for display and rule evaluation. */
    Map<UUID, String> roleCodes();
}
