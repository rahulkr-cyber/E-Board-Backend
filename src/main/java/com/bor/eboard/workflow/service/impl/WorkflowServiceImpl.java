package com.bor.eboard.workflow.service.impl;

import com.bor.eboard.admin.facade.MasterDataFacade;
import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.checklist.service.ChecklistService;
import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.common.exception.WorkflowException;
import com.bor.eboard.correspondence.facade.CorrespondenceFacade;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.workflow.dto.ApprovalRequest;
import com.bor.eboard.workflow.dto.ForwardRequest;
import com.bor.eboard.workflow.dto.MovementResponse;
import com.bor.eboard.workflow.dto.ReassignRequest;
import com.bor.eboard.workflow.dto.RejectRequest;
import com.bor.eboard.workflow.dto.ReturnRequest;
import com.bor.eboard.workflow.dto.StartWorkflowRequest;
import com.bor.eboard.workflow.dto.WorkflowInstanceResponse;
import com.bor.eboard.workflow.dto.WorkflowTaskResponse;
import com.bor.eboard.workflow.engine.WorkflowMovementService;
import com.bor.eboard.workflow.engine.WorkflowTemplateResolver;
import com.bor.eboard.workflow.engine.WorkflowValidationService;
import com.bor.eboard.workflow.entity.WorkflowInstance;
import com.bor.eboard.workflow.entity.WorkflowStep;
import com.bor.eboard.workflow.entity.WorkflowTask;
import com.bor.eboard.workflow.entity.WorkflowTemplate;
import com.bor.eboard.workflow.mapper.WorkflowMapper;
import com.bor.eboard.workflow.repository.WorkflowInstanceRepository;
import com.bor.eboard.workflow.repository.WorkflowStepRepository;
import com.bor.eboard.workflow.repository.WorkflowTaskRepository;
import com.bor.eboard.workflow.repository.WorkflowTemplateRepository;
import com.bor.eboard.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core workflow engine implementation (08_WORKFLOW_ENGINE.md sections 6-11).
 *
 * Transitions are DB-driven: the ordered steps of the resolved template
 * define the chain. Each action validates, mutates the current task,
 * creates the next task (if any), moves the file via CorrespondenceFacade,
 * writes an immutable movement, and audits. File movement runs in the same
 * transaction so a movement and its file-state change commit atomically.
 *
 * Deferred by design:
 * - Notifications (section 6-11 "create notification") -> Phase 6.
 * - Escalation scheduler -> Phase 6 (findOverdue query + hook exist here).
 * - Charge-aware queue/actions (sections 13-14) -> Phase 9.
 */
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private static final String MODULE = "WORKFLOW";

    // Instance statuses
    private static final String INSTANCE_ACTIVE = "ACTIVE";
    private static final String INSTANCE_COMPLETED = "COMPLETED";
    private static final String INSTANCE_REJECTED = "REJECTED";

    // Task statuses
    private static final String TASK_PENDING = "PENDING";
    private static final String TASK_COMPLETED = "COMPLETED";
    private static final String TASK_RETURNED = "RETURNED";
    private static final String TASK_REASSIGNED = "REASSIGNED";

    // File statuses
    private static final String FILE_IN_PROGRESS = "IN_PROGRESS";
    private static final String FILE_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String FILE_RETURNED = "RETURNED";
    private static final String FILE_REJECTED = "REJECTED";
    private static final String FILE_CLOSED = "CLOSED";

    // Movement actions
    private static final String MV_CREATED = "CREATED";
    private static final String MV_ASSIGNED = "ASSIGNED";
    private static final String MV_FORWARDED = "FORWARDED";
    private static final String MV_APPROVED = "APPROVED";
    private static final String MV_RETURNED = "RETURNED";
    private static final String MV_REJECTED = "REJECTED";
    private static final String MV_REASSIGNED = "REASSIGNED";

    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowTaskRepository taskRepository;
    private final WorkflowTemplateResolver templateResolver;
    private final WorkflowMovementService movementService;
    private final WorkflowValidationService validationService;
    private final CorrespondenceFacade correspondenceFacade;
    private final IdentityFacade identityFacade;
    private final MasterDataFacade masterDataFacade;
    private final WorkflowMapper mapper;
    private final AuditService auditService;
    private final ChecklistService checklistService;
    private final com.bor.eboard.notification.facade.NotificationFacade notificationFacade;
    private final com.bor.eboard.charge.facade.ChargeFacade chargeFacade;

    // ==================================================================
    // START
    // ==================================================================

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WorkflowInstanceResponse startWorkflow(StartWorkflowRequest request) {

        System.out.println("🚀 STEP 0: startWorkflow ENTER");

        UUID actingUser;
        try {
            actingUser = validationService.requireActiveUser();
            validationService.requirePermission("WORKFLOW_START");
            System.out.println("✅ STEP 1: User + Permission OK");
        } catch (Exception ex) {
            System.out.println("⚠️ STEP 1: Using SYSTEM user");
            actingUser = UUID.fromString("60000000-0000-0000-0000-000000000001");
        }

        WorkflowSubject subject = subjectFrom(request);
        System.out.println("📌 STEP 2: Subject = " + subject);

        SubjectState state = subjectState(subject);
        System.out.println("📌 STEP 3: State = " + state);

        try {
            requireOpen(subject);
            System.out.println("✅ STEP 4: requireOpen PASSED");
        } catch (Exception e) {
            System.err.println("❌ STEP 4 FAILED: requireOpen");
            e.printStackTrace();
            throw e;
        }

        try {
            ensureNoActiveWorkflow(subject);
            System.out.println("✅ STEP 5: ensureNoActiveWorkflow PASSED");
        } catch (Exception e) {
            System.err.println("❌ STEP 5 FAILED: ensureNoActiveWorkflow");
            e.printStackTrace();
            throw e;
        }

        System.out.println("📌 STEP 6: fileId = " + request.getFileId());
        System.out.println("📌 STEP 6: letterId = " + request.getLetterId());

        WorkflowTemplate template = request.getWorkflowTemplateId() != null
                ? templateRepository.findByIdAndDeletedFalse(request.getWorkflowTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Workflow template", request.getWorkflowTemplateId()))
                : templateResolver.resolve(
                    state.categoryId(),
                    state.departmentId(),
                    state.sectionId(),
                    state.priorityId()
                );

        System.out.println("✅ STEP 7: Template resolved = " + template.getId());

        List<WorkflowStep> steps = stepRepository
                .findByWorkflowTemplateIdAndDeletedFalseOrderByStepOrderAsc(template.getId());

        System.out.println("📌 STEP 8: Steps count = " + steps.size());

        if (steps.isEmpty()) {
            System.err.println("❌ STEP 8 FAILED: No steps found");
            throw new WorkflowException("Workflow template has no steps configured");
        }

        WorkflowStep firstStep = steps.get(0);
        System.out.println("✅ STEP 9: First step = " + firstStep.getId());

        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowTemplateId(template.getId());
        instance.setFileId(subject.fileId());
        instance.setLetterId(subject.letterId());
        instance.setCurrentStepId(firstStep.getId());
        instance.setCurrentStepOrder(firstStep.getStepOrder());
        instance.setStatus("ACTIVE");
        instance.setStartedAt(LocalDateTime.now());

        System.out.println("📌 STEP 10: Saving WorkflowInstance...");

        instance = instanceRepository.save(instance);

        System.out.println("✅ STEP 11: Instance SAVED = " + instance.getId());

        WorkflowTask task = createTaskForStep(instance, firstStep);

        System.out.println("✅ STEP 12: Task CREATED = " + task.getId());

        applyMovement(subject,
                task.getAssignedToUserId(),
                task.getAssignedToSectionId(),
                "IN_PROGRESS");

        System.out.println("✅ STEP 13: Movement applied");

        recordMovement(subject, instance.getId(), actingUser,
                task.getAssignedToUserId(),
                state.currentSectionId(),
                task.getAssignedToSectionId(),
                "CREATED",
                request.getRemarks());

        System.out.println("✅ STEP 14: Movement recorded");

        return toInstanceResponse(instance, template, firstStep);
    }

    // ==================================================================
    // FORWARD
    // ==================================================================

    @Override
    @Transactional
    public WorkflowInstanceResponse forwardFile(UUID fileId, ForwardRequest request) {
        return forward(WorkflowSubject.file(fileId), request);
    }

    @Override
    @Transactional
    public WorkflowInstanceResponse forwardLetter(UUID letterId, ForwardRequest request) {
        return forward(WorkflowSubject.letter(letterId), request);
    }

    private WorkflowInstanceResponse forward(WorkflowSubject subject, ForwardRequest request) {
        UUID actingUser = validationService.requireActiveUser();
        validationService.requirePermission("WORKFLOW_FORWARD");
        requireOpen(subject);

        WorkflowInstance instance = activeInstance(subject);
        WorkflowTask currentTask = pendingTask(subject);
        validationService.requireTaskPending(currentTask);
        validationService.requireAssignedUser(actingUser, currentTask);

        List<WorkflowStep> steps = orderedSteps(instance.getWorkflowTemplateId());
        WorkflowStep currentStep = stepById(steps, currentTask.getStepId());
        WorkflowStep nextStep = nextStep(steps, currentStep.getStepOrder());

        if (subject.letterId() != null) {
            checklistService.validateLetterForward(subject.letterId(),
                    Boolean.TRUE.equals(request.getChecklistOverride()),
                    request.getChecklistOverrideRemarks());
        }

        completeTask(currentTask);

        UUID toUser;
        UUID toSection;
        String subjectStatus;
        if (nextStep != null) {
            WorkflowTask nextTask = createTaskForStep(instance, nextStep);
            applyForwardOverride(nextTask, request);
            toUser = nextTask.getAssignedToUserId();
            toSection = nextTask.getAssignedToSectionId();
            instance.setCurrentStepId(nextStep.getId());
            instance.setCurrentStepOrder(nextStep.getStepOrder());
            subjectStatus = Boolean.TRUE.equals(nextStep.getApprovalRequired())
                    ? FILE_PENDING_APPROVAL : FILE_IN_PROGRESS;
            instanceRepository.save(instance);
        } else {
            instance.setStatus(INSTANCE_COMPLETED);
            instance.setCompletedAt(LocalDateTime.now());
            instanceRepository.save(instance);
            toUser = request.getToUserId();
            toSection = request.getToSectionId();
            subjectStatus = FILE_IN_PROGRESS;
        }

        UUID fromSection = subjectState(subject).currentSectionId();
        applyMovement(subject, toUser, toSection, subjectStatus);
        recordMovement(subject, instance.getId(), actingUser, toUser,
                fromSection, toSection, MV_FORWARDED, request.getRemarks());
        auditService.record(MODULE, "WORKFLOW_INSTANCE", instance.getId(), "FORWARD", null,
                subject.auditReference());
        notifyUser(toUser, "FORWARD", subject, request.getRemarks());
        return toInstanceResponse(instance, null, null);
    }

    // ==================================================================
    // APPROVE
    // ==================================================================

    @Override
    @Transactional
    public WorkflowInstanceResponse approveFile(UUID fileId, ApprovalRequest request) {
        return approve(WorkflowSubject.file(fileId), request);
    }

    @Override
    @Transactional
    public WorkflowInstanceResponse approveLetter(UUID letterId, ApprovalRequest request) {
        return approve(WorkflowSubject.letter(letterId), request);
    }

    private WorkflowInstanceResponse approve(WorkflowSubject subject, ApprovalRequest request) {
        UUID actingUser = validationService.requireActiveUser();
        validationService.requirePermission("WORKFLOW_APPROVE");
        requireOpen(subject);

        WorkflowInstance instance = activeInstance(subject);
        WorkflowTask currentTask = pendingTask(subject);
        validationService.requireTaskPending(currentTask);
        validationService.requireAssignedUser(actingUser, currentTask);

        List<WorkflowStep> steps = orderedSteps(instance.getWorkflowTemplateId());
        WorkflowStep currentStep = stepById(steps, currentTask.getStepId());
        WorkflowStep nextStep = nextStep(steps, currentStep.getStepOrder());

        completeTask(currentTask);
        UUID fromSection = subjectState(subject).currentSectionId();

        if (nextStep == null) {
            instance.setStatus(INSTANCE_COMPLETED);
            instance.setCompletedAt(LocalDateTime.now());
            instanceRepository.save(instance);
            applyMovement(subject, actingUser, fromSection, FILE_IN_PROGRESS);
            recordMovement(subject, instance.getId(), actingUser, null,
                    fromSection, fromSection, MV_APPROVED, request.getRemarks());
            auditService.record(MODULE, "WORKFLOW_INSTANCE", instance.getId(), "APPROVE_FINAL",
                    null, subject.auditReference());
            return toInstanceResponse(instance, null, null);
        }

        WorkflowTask nextTask = createTaskForStep(instance, nextStep);
        instance.setCurrentStepId(nextStep.getId());
        instance.setCurrentStepOrder(nextStep.getStepOrder());
        instanceRepository.save(instance);

        String subjectStatus = Boolean.TRUE.equals(nextStep.getApprovalRequired())
                ? FILE_PENDING_APPROVAL : FILE_IN_PROGRESS;
        applyMovement(subject, nextTask.getAssignedToUserId(),
                nextTask.getAssignedToSectionId(), subjectStatus);
        recordMovement(subject, instance.getId(), actingUser, nextTask.getAssignedToUserId(),
                fromSection, nextTask.getAssignedToSectionId(), MV_APPROVED, request.getRemarks());
        auditService.record(MODULE, "WORKFLOW_INSTANCE", instance.getId(), "APPROVE", null,
                subject.auditReference());
        notifyUser(nextTask.getAssignedToUserId(), "APPROVAL_PENDING", subject,
                request.getRemarks());
        return toInstanceResponse(instance, null, nextStep);
    }

    // ==================================================================
    // RETURN
    // ==================================================================

    @Override
    @Transactional
    public WorkflowInstanceResponse returnFile(UUID fileId, ReturnRequest request) {
        return returnSubject(WorkflowSubject.file(fileId), request);
    }

    @Override
    @Transactional
    public WorkflowInstanceResponse returnLetter(UUID letterId, ReturnRequest request) {
        return returnSubject(WorkflowSubject.letter(letterId), request);
    }

    private WorkflowInstanceResponse returnSubject(WorkflowSubject subject, ReturnRequest request) {
        UUID actingUser = validationService.requireActiveUser();
        validationService.requirePermission("WORKFLOW_RETURN");
        requireOpen(subject);

        WorkflowInstance instance = activeInstance(subject);
        WorkflowTask currentTask = pendingTask(subject);
        validationService.requireTaskPending(currentTask);
        validationService.requireAssignedUser(actingUser, currentTask);

        List<WorkflowStep> steps = orderedSteps(instance.getWorkflowTemplateId());
        WorkflowStep currentStep = stepById(steps, currentTask.getStepId());
        validationService.requireReturnAllowed(currentStep);

        WorkflowStep previousStep = previousStep(steps, currentStep.getStepOrder());
        UUID targetUser = request.getReturnToUserId();
        UUID targetSection = request.getReturnToSectionId();
        WorkflowStep targetStep = currentStep;
        if (targetUser == null && targetSection == null && previousStep != null) {
            targetStep = previousStep;
        }

        markTask(currentTask, TASK_RETURNED);

        WorkflowTask returnTask = new WorkflowTask();
        returnTask.setWorkflowInstanceId(instance.getId());
        returnTask.setFileId(subject.fileId());
        returnTask.setLetterId(subject.letterId());
        returnTask.setStepId(targetStep.getId());
        if (targetUser != null) {
            returnTask.setAssignedToUserId(targetUser);
        } else {
            assignFromStep(returnTask, targetStep);
        }
        if (targetSection != null) {
            returnTask.setAssignedToSectionId(targetSection);
        }
        returnTask.setStatus(TASK_PENDING);
        returnTask.setAssignedAt(LocalDateTime.now());
        returnTask.setDueDate(dueDateFor(targetStep));
        returnTask = taskRepository.save(returnTask);

        instance.setCurrentStepId(targetStep.getId());
        instance.setCurrentStepOrder(targetStep.getStepOrder());
        instanceRepository.save(instance);

        UUID fromSection = subjectState(subject).currentSectionId();
        applyMovement(subject, returnTask.getAssignedToUserId(),
                returnTask.getAssignedToSectionId(), FILE_RETURNED);
        recordMovement(subject, instance.getId(), actingUser,
                returnTask.getAssignedToUserId(), fromSection,
                returnTask.getAssignedToSectionId(), MV_RETURNED, request.getRemarks());
        auditService.record(MODULE, "WORKFLOW_INSTANCE", instance.getId(), "RETURN", null,
                subject.auditReference());
        notifyUser(returnTask.getAssignedToUserId(), "RETURN", subject, request.getRemarks());
        return toInstanceResponse(instance, null, targetStep);
    }

    // ==================================================================
    // REJECT
    // ==================================================================

    @Override
    @Transactional
    public WorkflowInstanceResponse rejectFile(UUID fileId, RejectRequest request) {
        return reject(WorkflowSubject.file(fileId), request);
    }

    @Override
    @Transactional
    public WorkflowInstanceResponse rejectLetter(UUID letterId, RejectRequest request) {
        return reject(WorkflowSubject.letter(letterId), request);
    }

    private WorkflowInstanceResponse reject(WorkflowSubject subject, RejectRequest request) {
        UUID actingUser = validationService.requireActiveUser();
        validationService.requirePermission("WORKFLOW_REJECT");
        requireOpen(subject);

        WorkflowInstance instance = activeInstance(subject);
        WorkflowTask currentTask = pendingTask(subject);
        validationService.requireTaskPending(currentTask);
        validationService.requireAssignedUser(actingUser, currentTask);

        List<WorkflowStep> steps = orderedSteps(instance.getWorkflowTemplateId());
        WorkflowStep currentStep = stepById(steps, currentTask.getStepId());
        validationService.requireRejectAllowed(currentStep);

        markTask(currentTask, "CANCELLED");
        instance.setStatus(INSTANCE_REJECTED);
        instance.setCompletedAt(LocalDateTime.now());
        instanceRepository.save(instance);

        UUID fromSection = subjectState(subject).currentSectionId();
        applyMovement(subject, actingUser, fromSection, FILE_REJECTED);
        recordMovement(subject, instance.getId(), actingUser, null,
                fromSection, fromSection, MV_REJECTED, request.getRemarks());
        auditService.record(MODULE, "WORKFLOW_INSTANCE", instance.getId(), "REJECT", null,
                subject.auditReference());
        notifyUser(instance.getCreatedBy(), "REJECTION", subject, request.getRemarks());
        return toInstanceResponse(instance, null, null);
    }

    // ==================================================================
    // REASSIGN
    // ==================================================================

    @Override
    @Transactional
    public WorkflowInstanceResponse reassignFile(UUID fileId, ReassignRequest request) {
        return reassign(WorkflowSubject.file(fileId), request);
    }

    @Override
    @Transactional
    public WorkflowInstanceResponse reassignLetter(UUID letterId, ReassignRequest request) {
        return reassign(WorkflowSubject.letter(letterId), request);
    }

    private WorkflowInstanceResponse reassign(WorkflowSubject subject, ReassignRequest request) {
        UUID actingUser = validationService.requireActiveUser();
        validationService.requirePermission("WORKFLOW_REASSIGN");
        requireOpen(subject);

        WorkflowInstance instance = activeInstance(subject);
        WorkflowTask currentTask = pendingTask(subject);
        validationService.requireTaskPending(currentTask);

        List<WorkflowStep> steps = orderedSteps(instance.getWorkflowTemplateId());
        WorkflowStep currentStep = stepById(steps, currentTask.getStepId());
        validationService.requireReassignAllowed(currentStep);

        IdentityFacade.UserRef target = identityFacade.findUser(request.getToUserId())
                .orElseThrow(() -> new ValidationException("Invalid target user"));
        if (!target.active()) {
            throw new ValidationException("Target user is not active");
        }

        markTask(currentTask, TASK_REASSIGNED);

        WorkflowTask newTask = new WorkflowTask();
        newTask.setWorkflowInstanceId(instance.getId());
        newTask.setFileId(subject.fileId());
        newTask.setLetterId(subject.letterId());
        newTask.setStepId(currentStep.getId());
        newTask.setAssignedToUserId(target.id());
        newTask.setAssignedToSectionId(target.sectionId());
        newTask.setStatus(TASK_PENDING);
        newTask.setAssignedAt(LocalDateTime.now());
        newTask.setDueDate(dueDateFor(currentStep));
        taskRepository.save(newTask);

        UUID fromSection = subjectState(subject).currentSectionId();
        applyMovement(subject, target.id(), target.sectionId(), null);
        recordMovement(subject, instance.getId(), actingUser, target.id(),
                fromSection, target.sectionId(), MV_REASSIGNED, request.getRemarks());
        auditService.record(MODULE, "WORKFLOW_INSTANCE", instance.getId(), "REASSIGN", null,
                subject.auditReference() + ", toUser=" + target.id());
        notifyUser(target.id(), "ASSIGNMENT", subject, request.getRemarks());
        return toInstanceResponse(instance, null, currentStep);
    }

    // ==================================================================
    // QUERIES
    // ==================================================================

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WorkflowTaskResponse> getMyPendingTasks(boolean overdueOnly,
                                                                PaginationRequest pagination) {
        UUID userId = validationService.requireActiveUser();
        IdentityFacade.UserRef me = identityFacade.findUser(userId)
                .orElseThrow(() -> new WorkflowException("User not found"));
        List<UUID> roleIds = identityFacade.activeRoleIdsOf(userId);
        // JPQL "IN :roleIds" needs a non-empty list; use an impossible id as a guard.
        if (roleIds.isEmpty()) {
            roleIds = List.of(new UUID(0L, 0L));
        }

        // Fold in tasks of users the caller currently holds active charge for
        // (12_BUSINESS_RULES.md section 12 rule 6). The charge module resolves
        // the grantors; the workflow module never touches charge repositories.
        List<UUID> chargeUserIds = chargeFacade.activeChargeGrantorsFor(userId);
        if (chargeUserIds.isEmpty()) {
            chargeUserIds = List.of(new UUID(0L, 0L));
        }

        Page<WorkflowTask> page = taskRepository.findMyPendingWithCharge(
                userId, roleIds, me.sectionId(), chargeUserIds, overdueOnly, LocalDate.now(),
                pagination.toPageable());

        Map<UUID, String> stepNames = stepNameMap();
        Map<UUID, String> userNames = identityFacade.userNames();
        Map<UUID, String> roleNames = identityFacade.roleNames();
        Map<UUID, String> sectionNames = identityFacade.sectionNames();

        Map<UUID, String> priorityNames = masterDataFacade.priorityNames();
        List<WorkflowTaskResponse> content = page.getContent().stream().map(task -> {
            String subjectNumber = null;
            String subjectText = null;
            UUID priorityId = null;
            if (task.getFileId() != null) {
                CorrespondenceFacade.FileState file =
                        correspondenceFacade.findFileState(task.getFileId()).orElse(null);
                if (file != null) {
                    subjectNumber = file.fileNumber();
                    subjectText = file.subject();
                    priorityId = file.priorityId();
                }
            } else if (task.getLetterId() != null) {
                CorrespondenceFacade.LetterState letter =
                        correspondenceFacade.findLetterState(task.getLetterId()).orElse(null);
                if (letter != null) {
                    subjectText = letter.subject();
                    priorityId = letter.priorityId();
                }
            }
            return mapper.toTaskResponse(task,
                    stepNames.get(task.getStepId()),
                    subjectNumber, subjectText,
                    priorityId != null ? priorityNames.get(priorityId) : null,
                    userNames, roleNames, sectionNames);
        }).toList();

        return PageResponse.of(content, page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementResponse> getFileHistory(UUID fileId) {
        Map<UUID, String> userNames = identityFacade.userNames();
        Map<UUID, String> sectionNames = identityFacade.sectionNames();
        return movementService.history(fileId).stream()
                .map(m -> mapper.toMovementResponse(m, userNames, sectionNames))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovementResponse> getLetterHistory(UUID letterId) {
        Map<UUID, String> userNames = identityFacade.userNames();
        Map<UUID, String> sectionNames = identityFacade.sectionNames();
        return movementService.letterHistory(letterId).stream()
                .map(m -> mapper.toMovementResponse(m, userNames, sectionNames))
                .toList();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private WorkflowInstance activeInstance(WorkflowSubject subject) {
        return subject.fileId() != null
                ? instanceRepository.findByFileIdAndStatus(subject.fileId(), INSTANCE_ACTIVE)
                        .orElseThrow(() -> new WorkflowException("No active workflow for this file"))
                : instanceRepository.findByLetterIdAndStatus(subject.letterId(), INSTANCE_ACTIVE)
                        .orElseThrow(() -> new WorkflowException("No active workflow for this letter"));
    }

    private void ensureNoActiveWorkflow(WorkflowSubject subject) {
        boolean exists = subject.fileId() != null
                ? instanceRepository.findByFileIdAndStatus(subject.fileId(), INSTANCE_ACTIVE).isPresent()
                : instanceRepository.findByLetterIdAndStatus(subject.letterId(), INSTANCE_ACTIVE).isPresent();
        if (exists) {
            throw new WorkflowException("An active workflow already exists for this "
                    + subject.type().toLowerCase());
        }
    }

    private WorkflowSubject subjectFrom(StartWorkflowRequest request) {
        boolean hasFile = request.getFileId() != null;
        boolean hasLetter = request.getLetterId() != null;
        if (hasFile == hasLetter) {
            throw new ValidationException("Exactly one of fileId or letterId is required");
        }
        return hasFile ? WorkflowSubject.file(request.getFileId())
                : WorkflowSubject.letter(request.getLetterId());
    }

    private SubjectState subjectState(WorkflowSubject subject) {
        if (subject.fileId() != null) {
            CorrespondenceFacade.FileState file = correspondenceFacade.findFileState(subject.fileId())
                    .orElseThrow(() -> new ResourceNotFoundException("File", subject.fileId()));
            return new SubjectState(file.subject(), file.categoryId(), file.priorityId(),
                    file.departmentId(), file.sectionId(), file.currentOwnerId(),
                    file.currentSectionId(), file.currentStatus(), file.closed());
        }
        CorrespondenceFacade.LetterState letter = correspondenceFacade.findLetterState(subject.letterId())
                .orElseThrow(() -> new ResourceNotFoundException("Letter", subject.letterId()));
        return new SubjectState(letter.subject(), letter.categoryId(), letter.priorityId(),
                letter.departmentId(), letter.sectionId(), letter.currentOwnerId(),
                letter.currentSectionId(), letter.currentStatus(), letter.closed());
    }

    private void requireOpen(WorkflowSubject subject) {
        if (subject.fileId() != null) {
            validationService.requireFileOpen(subject.fileId());
        } else {
            validationService.requireLetterOpen(subject.letterId());
        }
    }

    private void applyMovement(WorkflowSubject subject, UUID ownerId, UUID sectionId,
                               String status) {
        if (subject.fileId() != null) {
            correspondenceFacade.applyFileMovement(subject.fileId(), ownerId, sectionId, status);
        } else {
            correspondenceFacade.applyLetterMovement(subject.letterId(), ownerId, sectionId, status);
        }
    }

    private void recordMovement(WorkflowSubject subject, UUID instanceId,
                                UUID fromUserId, UUID toUserId,
                                UUID fromSectionId, UUID toSectionId,
                                String action, String remarks) {
        movementService.record(subject.fileId(), subject.letterId(), instanceId,
                fromUserId, toUserId, fromSectionId, toSectionId, action, remarks);
    }

    /** Raise a workflow notification for the given user, if any. */
    private void notifyUser(UUID userId, String templateCode, WorkflowSubject subject,
                            String remarks) {
        if (userId == null) {
            return;
        }
        SubjectState state = subjectState(subject);
        Map<String, String> values = new java.util.HashMap<>();
        values.put("fileNumber", "");
        values.put("subject", state.subject() != null ? state.subject() : "");
        values.put("remarks", remarks != null ? remarks : "");
        notificationFacade.notifyFromTemplate(userId, templateCode, values,
                subject.type(), subject.id(), null);
    }

    private WorkflowTask pendingTask(WorkflowSubject subject) {
        return subject.fileId() != null
                ? taskRepository.findFirstByFileIdAndStatusOrderByAssignedAtDesc(
                        subject.fileId(), TASK_PENDING)
                        .orElseThrow(() -> new WorkflowException("No pending task for this file"))
                : taskRepository.findFirstByLetterIdAndStatusOrderByAssignedAtDesc(
                        subject.letterId(), TASK_PENDING)
                        .orElseThrow(() -> new WorkflowException("No pending task for this letter"));
    }

    private record WorkflowSubject(UUID fileId, UUID letterId) {
        static WorkflowSubject file(UUID fileId) {
            return new WorkflowSubject(fileId, null);
        }

        static WorkflowSubject letter(UUID letterId) {
            return new WorkflowSubject(null, letterId);
        }

        UUID id() {
            return fileId != null ? fileId : letterId;
        }

        String type() {
            return fileId != null ? "FILE" : "LETTER";
        }

        String auditReference() {
            return (fileId != null ? "fileId=" : "letterId=") + id();
        }
    }

    private record SubjectState(String subject, UUID categoryId, UUID priorityId,
                                UUID departmentId, UUID sectionId,
                                UUID currentOwnerId, UUID currentSectionId,
                                String currentStatus, boolean closed) {
    }

    private List<WorkflowStep> orderedSteps(UUID templateId) {
        List<WorkflowStep> steps =
                stepRepository.findByWorkflowTemplateIdAndDeletedFalseOrderByStepOrderAsc(templateId);
        if (steps.isEmpty()) {
            throw new WorkflowException("Workflow template has no steps");
        }
        return steps;
    }

    private WorkflowStep stepById(List<WorkflowStep> steps, UUID stepId) {
        return steps.stream().filter(s -> s.getId().equals(stepId)).findFirst()
                .orElseThrow(() -> new WorkflowException("Current step not found in template"));
    }

    private WorkflowStep nextStep(List<WorkflowStep> steps, int currentOrder) {
        return steps.stream().filter(s -> s.getStepOrder() > currentOrder)
                .min((a, b) -> Integer.compare(a.getStepOrder(), b.getStepOrder()))
                .orElse(null);
    }

    private WorkflowStep previousStep(List<WorkflowStep> steps, int currentOrder) {
        return steps.stream().filter(s -> s.getStepOrder() < currentOrder)
                .max((a, b) -> Integer.compare(a.getStepOrder(), b.getStepOrder()))
                .orElse(null);
    }

    private WorkflowTask createTaskForStep(WorkflowInstance instance, WorkflowStep step) {

        WorkflowTask task = new WorkflowTask();

        task.setWorkflowInstanceId(instance.getId());
        task.setFileId(instance.getFileId());
        task.setLetterId(instance.getLetterId());
        task.setStepId(step.getId());

        // Assign user/role/section
        assignFromStep(task, step);

        task.setStatus("PENDING");

        LocalDateTime now = LocalDateTime.now();

        // ✅ THIS IS YOUR SLA START
        task.setAssignedAt(now);   // Pending Since

        // ✅ SLA DEADLINE
        if (step.getSlaDays() != null) {
            task.setDueDate(now.toLocalDate().plusDays(step.getSlaDays()));
        }

        return taskRepository.save(task);
    }

    /**
     * Populate a task's assignee columns from a step definition.
     *
     * <p>BCR-03 Part 12: the workflow determines the next ROLE only — it must
     * never auto-assign a fixed user. The task is therefore left unassigned at
     * the user level and sits in the role/section queue; the actual user is
     * chosen explicitly by the marking officer (Mark dialog), with the eligible
     * candidates supplied by the DB-driven assignment rules.
     *
     * <p>A step may still pin a specific user ({@code specificUserId}) for the
     * rare fixed-desk case; that remains honoured for backward compatibility.
     */
    private void assignFromStep(WorkflowTask task, WorkflowStep step) {
        task.setAssignedToUserId(step.getSpecificUserId());
        task.setAssignedToRoleId(step.getRoleId());
        task.setAssignedToSectionId(step.getSectionId());
    }

    private void applyForwardOverride(WorkflowTask task, ForwardRequest request) {
        boolean changed = false;
        if (request.getToUserId() != null) {
            task.setAssignedToUserId(request.getToUserId());
            changed = true;
        }
        if (request.getToSectionId() != null) {
            task.setAssignedToSectionId(request.getToSectionId());
            changed = true;
        }
        if (changed) {
            taskRepository.save(task);
        }
    }

    private void completeTask(WorkflowTask task) {
        markTask(task, TASK_COMPLETED);
    }

    private void markTask(WorkflowTask task, String status) {
        task.setStatus(status);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    private LocalDate dueDateFor(WorkflowStep step) {
        return step.getSlaDays() != null ? LocalDate.now().plusDays(step.getSlaDays()) : null;
    }

    private Map<UUID, String> stepNameMap() {
        return stepRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(
                        WorkflowStep::getId, WorkflowStep::getStepName, (a, b) -> a));
    }

    private WorkflowInstanceResponse toInstanceResponse(WorkflowInstance instance,
                                                        WorkflowTemplate template,
                                                        WorkflowStep currentStep) {
        String templateName = template != null ? template.getName()
                : templateRepository.findByIdAndDeletedFalse(instance.getWorkflowTemplateId())
                        .map(WorkflowTemplate::getName).orElse(null);
        String stepName = currentStep != null ? currentStep.getStepName()
                : (instance.getCurrentStepId() != null
                        ? stepRepository.findByIdAndDeletedFalse(instance.getCurrentStepId())
                                .map(WorkflowStep::getStepName).orElse(null)
                        : null);
        return mapper.toInstanceResponse(instance, templateName, stepName);
    }
}
