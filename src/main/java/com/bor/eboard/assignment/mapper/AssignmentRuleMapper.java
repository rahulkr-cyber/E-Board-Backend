package com.bor.eboard.assignment.mapper;

import com.bor.eboard.assignment.dto.AssignmentRuleResponse;
import com.bor.eboard.assignment.entity.AssignmentRule;
import com.bor.eboard.identity.facade.IdentityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AssignmentRuleMapper {

    private final IdentityFacade identityFacade;

    public AssignmentRuleResponse toResponse(AssignmentRule rule) {
        Map<UUID, String> roles = identityFacade.roleNames();
        Map<UUID, String> sections = identityFacade.sectionNames();
        return AssignmentRuleResponse.builder()
                .id(rule.getId())
                .fromRoleId(rule.getFromRoleId())
                .fromRoleName(roles.get(rule.getFromRoleId()))
                .toRoleId(rule.getToRoleId())
                .toRoleName(roles.get(rule.getToRoleId()))
                .allowedSectionId(rule.getAllowedSectionId())
                .allowedSectionName(rule.getAllowedSectionId() == null
                        ? null : sections.get(rule.getAllowedSectionId()))
                .sameSectionAllowed(rule.getSameSectionAllowed())
                .crossSectionAllowed(rule.getCrossSectionAllowed())
                .multiUserAllowed(rule.getMultiUserAllowed())
                .active(rule.getActive())
                .remarks(rule.getRemarks())
                .build();
    }
}
