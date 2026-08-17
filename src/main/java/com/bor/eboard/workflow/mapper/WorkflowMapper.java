package com.bor.eboard.workflow.mapper;

import com.bor.eboard.workflow.dto.MovementResponse;
import com.bor.eboard.workflow.dto.WorkflowInstanceResponse;
import com.bor.eboard.workflow.dto.WorkflowStepResponse;
import com.bor.eboard.workflow.dto.WorkflowTaskResponse;
import com.bor.eboard.workflow.dto.WorkflowTemplateResponse;
import com.bor.eboard.workflow.entity.WorkflowInstance;
import com.bor.eboard.workflow.entity.WorkflowMovement;
import com.bor.eboard.workflow.entity.WorkflowStep;
import com.bor.eboard.workflow.entity.WorkflowTask;
import com.bor.eboard.workflow.entity.WorkflowTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manual mappers for the Workflow module. All reference names are supplied
 * via lookup maps assembled from the Identity / MasterData facades; there
 * are no entity associations.
 */
@Component
public class WorkflowMapper {

    public WorkflowTemplateResponse toTemplateResponse(WorkflowTemplate template,
                                                       List<WorkflowStepResponse> steps,
                                                       Map<UUID, String> categoryNames,
                                                       Map<UUID, String> departmentNames,
                                                       Map<UUID, String> sectionNames,
                                                       Map<UUID, String> priorityNames) {
        return WorkflowTemplateResponse.builder()
                .id(template.getId())
                .code(template.getCode())
                .name(template.getName())
                .description(template.getDescription())
                .categoryId(template.getCategoryId())
                .categoryName(name(categoryNames, template.getCategoryId()))
                .departmentId(template.getDepartmentId())
                .departmentName(name(departmentNames, template.getDepartmentId()))
                .sectionId(template.getSectionId())
                .sectionName(name(sectionNames, template.getSectionId()))
                .priorityId(template.getPriorityId())
                .priorityName(name(priorityNames, template.getPriorityId()))
                .active(template.getActive())
                .createdAt(template.getCreatedAt())
                .steps(steps)
                .build();
    }

    public WorkflowStepResponse toStepResponse(WorkflowStep step,
                                               Map<UUID, String> roleNames,
                                               Map<UUID, String> sectionNames,
                                               Map<UUID, String> userNames) {
        return WorkflowStepResponse.builder()
                .id(step.getId())
                .workflowTemplateId(step.getWorkflowTemplateId())
                .stepOrder(step.getStepOrder())
                .stepName(step.getStepName())
                .roleId(step.getRoleId())
                .roleName(name(roleNames, step.getRoleId()))
                .designationId(step.getDesignationId())
                .sectionId(step.getSectionId())
                .sectionName(name(sectionNames, step.getSectionId()))
                .specificUserId(step.getSpecificUserId())
                .specificUserName(name(userNames, step.getSpecificUserId()))
                .approvalRequired(step.getApprovalRequired())
                .canReturn(step.getCanReturn())
                .canReassign(step.getCanReassign())
                .canReject(step.getCanReject())
                .slaDays(step.getSlaDays())
                .parallelStep(step.getParallelStep())
                .build();
    }

    public WorkflowTaskResponse toTaskResponse(WorkflowTask task,
                                               String stepName,
                                               String fileNumber,
                                               String fileSubject,
                                               String priorityName,
                                               Map<UUID, String> userNames,
                                               Map<UUID, String> roleNames,
                                               Map<UUID, String> sectionNames) {
        boolean overdue = task.getDueDate() != null
                && task.getDueDate().isBefore(LocalDate.now())
                && "PENDING".equals(task.getStatus());
        return WorkflowTaskResponse.builder()
                .id(task.getId())
                .workflowInstanceId(task.getWorkflowInstanceId())
                .fileId(task.getFileId())
                .letterId(task.getLetterId())
                .fileNumber(fileNumber)
                .fileSubject(fileSubject)
                .stepId(task.getStepId())
                .stepName(stepName)
                .assignedToUserId(task.getAssignedToUserId())
                .assignedToUserName(name(userNames, task.getAssignedToUserId()))
                .assignedToRoleId(task.getAssignedToRoleId())
                .assignedToRoleName(name(roleNames, task.getAssignedToRoleId()))
                .assignedToSectionId(task.getAssignedToSectionId())
                .assignedToSectionName(name(sectionNames, task.getAssignedToSectionId()))
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .overdue(overdue)
                .priorityName(priorityName)
                .assignedAt(task.getAssignedAt())
                .build();
    }

    public MovementResponse toMovementResponse(WorkflowMovement movement,
                                               Map<UUID, String> userNames,
                                               Map<UUID, String> sectionNames) {
        return MovementResponse.builder()
                .id(movement.getId())
                .fileId(movement.getFileId())
                .workflowInstanceId(movement.getWorkflowInstanceId())
                .action(movement.getAction())
                .fromUserId(movement.getFromUserId())
                .fromUserName(name(userNames, movement.getFromUserId()))
                .toUserId(movement.getToUserId())
                .toUserName(name(userNames, movement.getToUserId()))
                .fromSectionId(movement.getFromSectionId())
                .fromSectionName(name(sectionNames, movement.getFromSectionId()))
                .toSectionId(movement.getToSectionId())
                .toSectionName(name(sectionNames, movement.getToSectionId()))
                .remarks(movement.getRemarks())
                .actionAt(movement.getActionAt())
                .actorName(name(userNames, movement.getCreatedBy()))
                .build();
    }

    public WorkflowInstanceResponse toInstanceResponse(WorkflowInstance instance,
                                                       String templateName,
                                                       String currentStepName) {
        return WorkflowInstanceResponse.builder()
                .id(instance.getId())
                .workflowTemplateId(instance.getWorkflowTemplateId())
                .workflowTemplateName(templateName)
                .fileId(instance.getFileId())
                .letterId(instance.getLetterId())
                .currentStepId(instance.getCurrentStepId())
                .currentStepOrder(instance.getCurrentStepOrder())
                .currentStepName(currentStepName)
                .status(instance.getStatus())
                .startedAt(instance.getStartedAt())
                .completedAt(instance.getCompletedAt())
                .build();
    }

    private String name(Map<UUID, String> names, UUID id) {
        return id != null ? names.get(id) : null;
    }
}
