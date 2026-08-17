package com.bor.eboard.assignment.service.impl;

import com.bor.eboard.assignment.dto.AssignmentRuleRequest;
import com.bor.eboard.assignment.dto.AssignmentRuleResponse;
import com.bor.eboard.assignment.dto.MarkTargetResponse;
import com.bor.eboard.assignment.entity.AssignmentRule;
import com.bor.eboard.assignment.mapper.AssignmentRuleMapper;
import com.bor.eboard.assignment.repository.AssignmentRuleRepository;
import com.bor.eboard.assignment.service.AssignmentService;
import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.identity.facade.IdentityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves who a user may mark work to, entirely from {@code assignment_rules}.
 * The workflow supplies the next ROLE; this service supplies the eligible USERS
 * (BCR-03 Parts 5 and 12). There is no hardcoded routing anywhere in Java.
 */
@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private static final String MODULE = "ASSIGNMENT";
    private static final String ENTITY = "ASSIGNMENT_RULE";

    private final AssignmentRuleRepository ruleRepository;
    private final AssignmentRuleMapper mapper;
    private final IdentityFacade identityFacade;
    private final AuditService auditService;

    // ------------------------------------------------------------------
    // Rule administration
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentRuleResponse> listRules() {
        return ruleRepository.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AssignmentRuleResponse createRule(AssignmentRuleRequest request) {
        validate(request);
        AssignmentRule rule = new AssignmentRule();
        apply(rule, request);
        AssignmentRule saved = ruleRepository.save(rule);
        auditService.record(MODULE, ENTITY, saved.getId(), "CREATE", null,
                "Assignment rule created");
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AssignmentRuleResponse updateRule(UUID id, AssignmentRuleRequest request) {
        validate(request);
        AssignmentRule rule = find(id);
        apply(rule, request);
        AssignmentRule saved = ruleRepository.save(rule);
        auditService.record(MODULE, ENTITY, saved.getId(), "UPDATE", null,
                "Assignment rule updated");
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AssignmentRuleResponse setRuleStatus(UUID id, boolean active) {
        AssignmentRule rule = find(id);
        String previous = String.valueOf(rule.getActive());
        rule.setActive(active);
        AssignmentRule saved = ruleRepository.save(rule);
        auditService.record(MODULE, ENTITY, saved.getId(), "STATUS_CHANGE",
                previous, String.valueOf(active));
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRule(UUID id) {
        AssignmentRule rule = find(id);
        rule.setDeleted(Boolean.TRUE);
        ruleRepository.save(rule);
        auditService.record(MODULE, ENTITY, id, "DELETE", null, "Assignment rule deleted");
    }

    // ------------------------------------------------------------------
    // Resolution for the Mark dialog
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public MarkTargetResponse markTargets(UUID toRoleId, UUID toSectionId) {
        UUID actingUser = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        IdentityFacade.UserRef me = identityFacade.findUser(actingUser)
                .orElseThrow(() -> new UnauthorizedException("Current user not found"));

        List<UUID> myRoles = identityFacade.activeRoleIdsOf(actingUser);
        System.out.println("Current User = " + actingUser);
        System.out.println("Roles = " + myRoles);
        if (myRoles.isEmpty()) {
            return MarkTargetResponse.builder()
                    .roles(List.of()).users(List.of()).multiUserAllowed(false).build();
        }

        List<AssignmentRule> rules = ruleRepository.findActiveForFromRoles(myRoles);
        System.out.println("Rules Count = " + rules.size());

        for (AssignmentRule r : rules) {
            System.out.println(
                r.getFromRoleId() + " -> " +
                r.getToRoleId() +
                " active=" + r.getActive() +
                " deleted=" + r.getDeleted()
            );
        }
        Map<UUID, String> roleNames = identityFacade.roleNames();
        Map<UUID, String> roleCodes = identityFacade.roleCodes();

        // Distinct target roles the caller may mark to.
        Map<UUID, MarkTargetResponse.RoleOption> roleOptions = new LinkedHashMap<>();
        for (AssignmentRule rule : rules) {
            roleOptions.computeIfAbsent(rule.getToRoleId(), rid ->
                    MarkTargetResponse.RoleOption.builder()
                            .roleId(rid)
                            .roleCode(roleCodes.get(rid))
                            .roleName(roleNames.get(rid))
                            .crossSectionAllowed(Boolean.TRUE.equals(rule.getCrossSectionAllowed()))
                            .build());
            // If any rule for this role allows cross-section, surface that.
            if (Boolean.TRUE.equals(rule.getCrossSectionAllowed())) {
                roleOptions.get(rule.getToRoleId()).setCrossSectionAllowed(Boolean.TRUE);
            }
        }

        // No role chosen yet: return only the role list.
        if (toRoleId == null) {
            return MarkTargetResponse.builder()
                    .roles(new ArrayList<>(roleOptions.values()))
                    .users(List.of())
                    .multiUserAllowed(Boolean.FALSE)
                    .build();
        }

        List<AssignmentRule> pairRules = ruleRepository.findActiveForPair(myRoles, toRoleId);
        if (pairRules.isEmpty()) {
            throw new ValidationException("You are not permitted to mark work to that role.");
        }

        UUID effectiveSection = resolveSection(pairRules, me.sectionId(), toSectionId);
        boolean multiUser = pairRules.stream()
                .anyMatch(r -> Boolean.TRUE.equals(r.getMultiUserAllowed()));

        Map<UUID, String> sectionNames = identityFacade.sectionNames();
        Map<UUID, String> designationNames = identityFacade.designationNames();

        List<MarkTargetResponse.UserOption> users = identityFacade
                .activeUsersByRole(toRoleId, effectiveSection).stream()
                .filter(u -> !u.id().equals(actingUser))   // never mark to yourself
                .map(u -> MarkTargetResponse.UserOption.builder()
                        .userId(u.id())
                        .fullName(u.fullName())
                        .designationName(u.designationId() == null
                                ? null : designationNames.get(u.designationId()))
                        .sectionId(u.sectionId())
                        .sectionName(u.sectionId() == null
                                ? null : sectionNames.get(u.sectionId()))
                        .build())
                .toList();

        return MarkTargetResponse.builder()
                .roles(new ArrayList<>(roleOptions.values()))
                .users(users)
                .multiUserAllowed(multiUser)
                .build();
    }

    /**
     * Applies the section reach flags. If the caller asked for a specific
     * section, it must be permitted; otherwise we default to the caller's own
     * section when cross-section marking is not allowed, and to "any section"
     * when it is.
     */
    private UUID resolveSection(List<AssignmentRule> rules, UUID mySection, UUID requestedSection) {
        boolean crossAllowed = rules.stream()
                .anyMatch(r -> Boolean.TRUE.equals(r.getCrossSectionAllowed()));
        boolean sameAllowed = rules.stream()
                .anyMatch(r -> Boolean.TRUE.equals(r.getSameSectionAllowed()));

        // A rule pinned to one section overrides everything else.
        UUID pinned = rules.stream()
                .map(AssignmentRule::getAllowedSectionId)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (pinned != null) {
            if (requestedSection != null && !pinned.equals(requestedSection)) {
                throw new ValidationException("Marking to that section is not permitted.");
            }
            return pinned;
        }

        if (requestedSection != null) {
            boolean isOwnSection = requestedSection.equals(mySection);
            if (isOwnSection && !sameAllowed) {
                throw new ValidationException("Marking within your own section is not permitted.");
            }
            if (!isOwnSection && !crossAllowed) {
                throw new ValidationException("Cross-section marking is not permitted for your role.");
            }
            return requestedSection;
        }

        // Nothing requested: widest permitted reach.
        return crossAllowed ? null : mySection;
    }

    // ------------------------------------------------------------------

    private AssignmentRule find(UUID id) {
        return ruleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment rule", id));
    }

    private void validate(AssignmentRuleRequest request) {
        if (!identityFacade.roleExists(request.getFromRoleId())) {
            throw new ValidationException("Invalid fromRoleId");
        }
        if (!identityFacade.roleExists(request.getToRoleId())) {
            throw new ValidationException("Invalid toRoleId");
        }
        if (request.getAllowedSectionId() != null
                && identityFacade.findSection(request.getAllowedSectionId()).isEmpty()) {
            throw new ValidationException("Invalid allowedSectionId");
        }
        if (!Boolean.TRUE.equals(request.getSameSectionAllowed())
                && !Boolean.TRUE.equals(request.getCrossSectionAllowed())) {
            throw new ValidationException(
                    "At least one of sameSectionAllowed or crossSectionAllowed must be true.");
        }
    }

    private void apply(AssignmentRule rule, AssignmentRuleRequest request) {
        rule.setFromRoleId(request.getFromRoleId());
        rule.setToRoleId(request.getToRoleId());
        rule.setAllowedSectionId(request.getAllowedSectionId());
        rule.setSameSectionAllowed(request.getSameSectionAllowed() != null
                ? request.getSameSectionAllowed() : Boolean.TRUE);
        rule.setCrossSectionAllowed(request.getCrossSectionAllowed() != null
                ? request.getCrossSectionAllowed() : Boolean.FALSE);
        rule.setMultiUserAllowed(request.getMultiUserAllowed() != null
                ? request.getMultiUserAllowed() : Boolean.FALSE);
        rule.setActive(request.getActive() != null ? request.getActive() : Boolean.TRUE);
        rule.setRemarks(request.getRemarks());
    }
}
