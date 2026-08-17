package com.bor.eboard.workflow.engine;

import com.bor.eboard.common.exception.WorkflowException;
import com.bor.eboard.workflow.entity.WorkflowTemplate;
import com.bor.eboard.workflow.repository.WorkflowTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Resolves the workflow template for a file by specificity
 * (08_WORKFLOW_ENGINE.md section 3):
 *   Category+Department+Section+Priority
 *   > Category+Department+Section
 *   > Category+Department
 *   > Category
 *   > Default (all selection dimensions null)
 *
 * A null selection dimension on a template is a wildcard; the most specific
 * matching template wins. No chain is hardcoded — everything is data.
 */
@Component
@RequiredArgsConstructor
public class WorkflowTemplateResolver {

    private final WorkflowTemplateRepository templateRepository;

    public WorkflowTemplate resolve(UUID categoryId, UUID departmentId,
                                    UUID sectionId, UUID priorityId) {
        List<WorkflowTemplate> candidates = templateRepository.findCandidates(
                categoryId, departmentId, sectionId, priorityId);
        if (candidates.isEmpty()) {
            throw new WorkflowException(
                    "No workflow template matches this file. Configure a template "
                    + "(a default template with no selection filters acts as a catch-all).");
        }
        return candidates.stream()
                .max(Comparator.comparingInt(this::specificity)
                        .thenComparing(WorkflowTemplate::getName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow(() -> new WorkflowException("Unable to resolve workflow template"));
    }

    /** Higher score = more specific match. Each concrete dimension adds weight. */
    private int specificity(WorkflowTemplate template) {
        int score = 0;
        if (template.getCategoryId() != null) score += 1;
        if (template.getDepartmentId() != null) score += 2;
        if (template.getSectionId() != null) score += 4;
        if (template.getPriorityId() != null) score += 8;
        return score;
    }
}
