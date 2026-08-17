package com.bor.eboard.workflow.service.impl;

import com.bor.eboard.admin.facade.MasterDataFacade;
import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.common.exception.DuplicateResourceException;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.workflow.dto.AddWorkflowStepRequest;
import com.bor.eboard.workflow.dto.CreateWorkflowTemplateRequest;
import com.bor.eboard.workflow.dto.WorkflowStepResponse;
import com.bor.eboard.workflow.dto.WorkflowTemplateResponse;
import com.bor.eboard.workflow.entity.WorkflowStep;
import com.bor.eboard.workflow.entity.WorkflowTemplate;
import com.bor.eboard.workflow.mapper.WorkflowMapper;
import com.bor.eboard.workflow.repository.WorkflowStepRepository;
import com.bor.eboard.workflow.repository.WorkflowTemplateRepository;
import com.bor.eboard.workflow.service.WorkflowTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowTemplateServiceImpl implements WorkflowTemplateService {

    private static final String MODULE = "WORKFLOW";

    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowStepRepository stepRepository;
    private final MasterDataFacade masterDataFacade;
    private final IdentityFacade identityFacade;
    private final WorkflowMapper mapper;
    private final AuditService auditService;

    @Override
    @Transactional
    public WorkflowTemplateResponse createTemplate(CreateWorkflowTemplateRequest request) {
        if (templateRepository.existsByCodeAndDeletedFalse(request.getCode())) {
            throw new DuplicateResourceException(
                    "A workflow template with code '" + request.getCode() + "' already exists");
        }
        validateSelectionReferences(request);

        WorkflowTemplate template = new WorkflowTemplate();
        template.setCode(request.getCode());
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setCategoryId(request.getCategoryId());
        template.setDepartmentId(request.getDepartmentId());
        template.setSectionId(request.getSectionId());
        template.setPriorityId(request.getPriorityId());
        template.setActive(Boolean.TRUE);
        template = templateRepository.save(template);

        auditService.record(MODULE, "WORKFLOW_TEMPLATE", template.getId(), "CREATE", null,
                "code=" + template.getCode());
        return assembleTemplate(template);
    }

    @Override
    @Transactional
    public WorkflowStepResponse addStep(UUID templateId, AddWorkflowStepRequest request) {
        WorkflowTemplate template = templateRepository.findByIdAndDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow template", templateId));

        if (stepRepository.existsByWorkflowTemplateIdAndStepOrderAndDeletedFalse(
                templateId, request.getStepOrder())) {
            throw new DuplicateResourceException(
                    "Step order " + request.getStepOrder() + " already exists in this template");
        }
        validateStepReferences(request);

        WorkflowStep step = new WorkflowStep();
        step.setWorkflowTemplateId(template.getId());
        step.setStepOrder(request.getStepOrder());
        step.setStepName(request.getStepName());
        step.setRoleId(request.getRoleId());
        step.setDesignationId(request.getDesignationId());
        step.setSectionId(request.getSectionId());
        step.setSpecificUserId(request.getSpecificUserId());
        step.setApprovalRequired(orDefault(request.getApprovalRequired(), true));
        step.setCanReturn(orDefault(request.getCanReturn(), true));
        step.setCanReassign(orDefault(request.getCanReassign(), true));
        step.setCanReject(orDefault(request.getCanReject(), true));
        step.setSlaDays(request.getSlaDays());
        step.setParallelStep(orDefault(request.getParallelStep(), false));
        step.setActive(Boolean.TRUE);
        step = stepRepository.save(step);

        auditService.record(MODULE, "WORKFLOW_STEP", step.getId(), "CREATE", null,
                "templateId=" + templateId + ", order=" + step.getStepOrder());
        return mapper.toStepResponse(step, identityFacade.roleNames(),
                identityFacade.sectionNames(), identityFacade.userNames());
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowTemplateResponse getTemplate(UUID templateId) {
        WorkflowTemplate template = templateRepository.findByIdAndDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow template", templateId));
        return assembleTemplate(template);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowTemplateResponse> listTemplates() {
        Map<UUID, String> categories = masterDataFacade.categoryNames();
        Map<UUID, String> priorities = masterDataFacade.priorityNames();
        Map<UUID, String> departments = identityFacade.departmentNames();
        Map<UUID, String> sections = identityFacade.sectionNames();
        return templateRepository.findByDeletedFalseOrderByNameAsc().stream()
                .map(t -> mapper.toTemplateResponse(t, List.of(),
                        categories, departments, sections, priorities))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowStepResponse> listSteps(UUID templateId) {
        Map<UUID, String> roleNames = identityFacade.roleNames();
        Map<UUID, String> sectionNames = identityFacade.sectionNames();
        Map<UUID, String> userNames = identityFacade.userNames();
        return stepRepository
                .findByWorkflowTemplateIdAndDeletedFalseOrderByStepOrderAsc(templateId).stream()
                .map(step -> mapper.toStepResponse(step, roleNames, sectionNames, userNames))
                .toList();
    }

    // ------------------------------------------------------------------

    private WorkflowTemplateResponse assembleTemplate(WorkflowTemplate template) {
        List<WorkflowStepResponse> steps = listSteps(template.getId());
        return mapper.toTemplateResponse(template, steps,
                masterDataFacade.categoryNames(), identityFacade.departmentNames(),
                identityFacade.sectionNames(), masterDataFacade.priorityNames());
    }

    private void validateSelectionReferences(CreateWorkflowTemplateRequest request) {
        if (request.getCategoryId() != null && !masterDataFacade.categoryExists(request.getCategoryId())) {
            throw new ValidationException("Invalid category reference");
        }
        if (request.getPriorityId() != null && !masterDataFacade.priorityExists(request.getPriorityId())) {
            throw new ValidationException("Invalid priority reference");
        }
        if (request.getDepartmentId() != null && !identityFacade.departmentExists(request.getDepartmentId())) {
            throw new ValidationException("Invalid department reference");
        }
        if (request.getSectionId() != null && identityFacade.findSection(request.getSectionId()).isEmpty()) {
            throw new ValidationException("Invalid section reference");
        }
    }

    private void validateStepReferences(AddWorkflowStepRequest request) {
        if (request.getRoleId() != null && !identityFacade.roleExists(request.getRoleId())) {
            throw new ValidationException("Invalid role reference");
        }
        if (request.getDesignationId() != null
                && !identityFacade.designationExists(request.getDesignationId())) {
            throw new ValidationException("Invalid designation reference");
        }
        if (request.getSectionId() != null
                && identityFacade.findSection(request.getSectionId()).isEmpty()) {
            throw new ValidationException("Invalid section reference");
        }
        if (request.getSpecificUserId() != null
                && identityFacade.findUser(request.getSpecificUserId()).isEmpty()) {
            throw new ValidationException("Invalid user reference");
        }
        if (request.getRoleId() == null && request.getSectionId() == null
                && request.getSpecificUserId() == null) {
            throw new ValidationException(
                    "A step must target at least a role, a section, or a specific user");
        }
    }

    private boolean orDefault(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
    }
}
