package com.bor.eboard.assignment.facade.impl;

import com.bor.eboard.assignment.entity.AssignmentRule;
import com.bor.eboard.assignment.facade.AssignmentFacade;
import com.bor.eboard.assignment.repository.AssignmentRuleRepository;
import com.bor.eboard.identity.facade.IdentityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentFacadeImpl implements AssignmentFacade {

    private final AssignmentRuleRepository ruleRepository;
    private final IdentityFacade identityFacade;

    @Override
    @Transactional(readOnly = true)
    public boolean canMark(List<UUID> fromRoleIds, UUID fromSectionId,
                           UUID toUserId, UUID toSectionId) {
        if (fromRoleIds == null || fromRoleIds.isEmpty() || toUserId == null) {
            return false;
        }
        List<UUID> targetRoles = identityFacade.activeRoleIdsOf(toUserId);
        if (targetRoles.isEmpty()) {
            return false;
        }

        List<AssignmentRule> rules = ruleRepository.findActiveForFromRoles(fromRoleIds);
        boolean sameSection = Objects.equals(fromSectionId, toSectionId);

        return rules.stream().anyMatch(rule -> {
            if (!targetRoles.contains(rule.getToRoleId())) {
                return false;
            }
            if (rule.getAllowedSectionId() != null
                    && !rule.getAllowedSectionId().equals(toSectionId)) {
                return false;
            }
            return sameSection
                    ? Boolean.TRUE.equals(rule.getSameSectionAllowed())
                    : Boolean.TRUE.equals(rule.getCrossSectionAllowed());
        });
    }
}
