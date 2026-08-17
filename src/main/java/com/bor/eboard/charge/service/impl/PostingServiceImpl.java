package com.bor.eboard.charge.service.impl;

import com.bor.eboard.audit.service.AuditService;
import com.bor.eboard.charge.dto.ChargeResponse;
import com.bor.eboard.charge.dto.CreatePostingRequest;
import com.bor.eboard.charge.dto.EmployeeHeaderResponse;
import com.bor.eboard.charge.dto.PostingResponse;
import com.bor.eboard.charge.dto.PostingTimelineEntryResponse;
import com.bor.eboard.charge.entity.JoiningRelieving;
import com.bor.eboard.charge.entity.TransferHistory;
import com.bor.eboard.charge.entity.UserPosting;
import com.bor.eboard.charge.mapper.ChargeMapper;
import com.bor.eboard.charge.mapper.PostingMapper;
import com.bor.eboard.charge.repository.ChargeAssignmentRepository;
import com.bor.eboard.charge.repository.JoiningRelievingRepository;
import com.bor.eboard.charge.repository.TransferHistoryRepository;
import com.bor.eboard.charge.repository.UserPostingRepository;
import com.bor.eboard.charge.service.PostingService;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.ValidationException;
import com.bor.eboard.filestorage.service.AttachmentStorageService;
import com.bor.eboard.identity.facade.IdentityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostingServiceImpl implements PostingService {

    private static final String MODULE = "POSTING";
    private static final Set<String> POSTING_TYPES = Set.of(
            "INITIAL_POSTING", "PROMOTION", "TRANSFER", "CHARGE_ASSIGNMENT",
            "ADDITIONAL_CHARGE", "JOINING", "RELIEVING", "DEPUTATION",
            "RETURN_FROM_DEPUTATION", "RETIREMENT");
    private static final Set<String> NON_SUBSTANTIVE_TYPES = Set.of(
            "ADDITIONAL_CHARGE", "CHARGE_ASSIGNMENT", "JOINING");

    private final UserPostingRepository postingRepository;
    private final TransferHistoryRepository transferRepository;
    private final JoiningRelievingRepository joiningRepository;
    private final ChargeAssignmentRepository chargeRepository;
    private final PostingMapper postingMapper;
    private final ChargeMapper chargeMapper;
    private final IdentityFacade identityFacade;
    private final AttachmentStorageService attachmentStorageService;
    private final AuditService auditService;

    @Override
    @Transactional
    public PostingTimelineEntryResponse create(CreatePostingRequest request) {
        String postingType = normalizeType(request.getPostingType());
        IdentityFacade.UserRef user = identityFacade.findUser(request.getUserId())
                .orElseThrow(() -> new ValidationException("Invalid employee"));
        validateDates(request);

        UUID departmentId = request.getDepartmentId() != null
                ? request.getDepartmentId() : user.departmentId();
        UUID sectionId = request.getSectionId() != null
                ? request.getSectionId() : user.sectionId();
        UUID designationId = request.getDesignationId() != null
                ? request.getDesignationId() : user.designationId();
        validateOrganization(departmentId, sectionId, designationId);
        validateEmployeeReference(request.getReportingOfficerId(), "reporting officer");
        validateEmployeeReference(request.getControllingOfficerId(), "controlling officer");
        validateEmployeeReference(request.getCurrentChargeHolderId(), "current charge holder");
        validateEmployeeReference(request.getPreviousChargeHolderId(), "previous charge holder");
        UserPosting previousPosting = currentSubstantivePosting(user.id()).orElse(null);
        IdentityFacade.EmployeeRef employee = identityFacade.findEmployee(user.id()).orElse(null);

        if (!NON_SUBSTANTIVE_TYPES.contains(postingType)) {
            closeActivePostings(user.id(), request.getEffectiveFrom().minusDays(1), "PAST");
        }

        UserPosting posting = new UserPosting();
        posting.setUserId(user.id());
        posting.setFromDepartmentId(user.departmentId());
        posting.setFromSectionId(user.sectionId());
        posting.setFromDesignationId(user.designationId());
        posting.setFromOfficeName(previousPosting == null ? null : previousPosting.getOfficeName());
        posting.setFromLocation(previousPosting == null ? null : previousPosting.getLocation());
        posting.setFromDistrict(previousPosting != null && StringUtils.hasText(previousPosting.getDistrict())
                ? previousPosting.getDistrict() : employee == null ? null : employee.district());
        posting.setDepartmentId(departmentId);
        posting.setSectionId(sectionId);
        posting.setDesignationId(designationId);
        posting.setPostingType(postingType);
        posting.setPostingStartDate(request.getEffectiveFrom());
        posting.setPostingEndDate(request.getEffectiveTo());
        posting.setOrderNumber(trim(request.getPostingOrderNumber()));
        posting.setOrderDate(request.getPostingOrderDate());
        posting.setGovernmentOrderNumber(trim(request.getGovernmentOrderNumber()));
        posting.setGovernmentOrderDate(request.getGovernmentOrderDate());
        posting.setOfficeName(trim(request.getOfficeName()));
        posting.setSeatName(trim(request.getSeatName()));
        posting.setReportingOfficerId(request.getReportingOfficerId());
        posting.setControllingOfficerId(request.getControllingOfficerId());
        posting.setLocation(trim(request.getLocation()));
        posting.setDistrict(trim(request.getDistrict()));
        posting.setTransferType(trim(request.getTransferType()));
        posting.setTransferReason(trim(request.getTransferReason()));
        posting.setPermanentCharge(request.getPermanentCharge() == null
                ? !"ADDITIONAL_CHARGE".equals(postingType) : request.getPermanentCharge());
        posting.setAdditionalCharge(request.getAdditionalCharge() == null
                ? "ADDITIONAL_CHARGE".equals(postingType) : request.getAdditionalCharge());
        posting.setCurrentChargeHolderId(request.getCurrentChargeHolderId());
        posting.setPreviousChargeHolderId(request.getPreviousChargeHolderId());
        posting.setChargeStartDate(request.getChargeStartDate());
        posting.setChargeEndDate(request.getChargeEndDate());
        posting.setChargeStatus(trim(request.getChargeStatus()));
        posting.setJoiningDate(request.getJoiningDate());
        posting.setJoiningTime(request.getJoiningTime());
        posting.setJoiningOrder(trim(request.getJoiningOrder()));
        posting.setJoiningRemarks(trim(request.getJoiningRemarks()));
        posting.setRelievingDate(request.getRelievingDate());
        posting.setRelievingTime(request.getRelievingTime());
        posting.setRelievingOrder(trim(request.getRelievingOrder()));
        posting.setRelievingRemarks(trim(request.getRelievingRemarks()));
        posting.setRemarks(trim(request.getRemarks()));
        posting.setStatus(statusFor(postingType, request.getEffectiveTo()));
        posting.setActive(request.getEffectiveTo() == null
                && !Set.of("RELIEVING", "RETIREMENT").contains(postingType));
        posting.setSourceEntityType("POSTING");
        posting = postingRepository.save(posting);

        if (!NON_SUBSTANTIVE_TYPES.contains(postingType)
                && !Set.of("RELIEVING", "RETIREMENT").contains(postingType)) {
            identityFacade.updateUserPosting(user.id(), departmentId, sectionId,
                    designationId, request.getDistrict());
            identityFacade.updateUserStatus(user.id(), "ACTIVE");
        } else if (Set.of("RELIEVING", "RETIREMENT").contains(postingType)) {
            identityFacade.updateUserStatus(user.id(), "INACTIVE");
        }

        auditService.record(MODULE, "USER_POSTING", posting.getId(), "CREATE", null,
                "user=" + user.id() + ", type=" + postingType
                        + ", effectiveFrom=" + request.getEffectiveFrom());
        return postingMapper.toResponse(posting);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostingTimelineEntryResponse> getByUser(UUID userId) {
        return postingRepository.findByUserIdAndDeletedFalseOrderByPostingStartDateDesc(userId)
                .stream().map(postingMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PostingResponse history(UUID userId) {
        IdentityFacade.EmployeeRef employee = identityFacade.findEmployee(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", userId));
        Map<UUID, String> departments = identityFacade.departmentNames();
        Map<UUID, String> sections = identityFacade.sectionNames();
        Map<UUID, String> designations = identityFacade.designationNames();

        List<com.bor.eboard.charge.dto.TransferResponse> transfers =
                transferRepository.findByUserIdOrderByTransferDateDesc(userId).stream()
                        .map(chargeMapper::toTransferResponse).toList();
        List<com.bor.eboard.charge.dto.JoiningRelievingResponse> events =
                joiningRepository.findByUserIdOrderByEventDateDesc(userId).stream()
                        .map(chargeMapper::toJoiningRelievingResponse).toList();
        List<ChargeResponse> held = chargeRepository
                .findByToUserIdAndDeletedFalseOrderByEffectiveFromDesc(userId).stream()
                .map(chargeMapper::toChargeResponse).toList();
        List<ChargeResponse> granted = chargeRepository
                .findByFromUserIdAndDeletedFalseOrderByEffectiveFromDesc(userId).stream()
                .map(chargeMapper::toChargeResponse).toList();

        List<UserPosting> detailedPostings = postingRepository
                .findByUserIdAndDeletedFalseOrderByPostingStartDateDesc(userId);
        List<PostingTimelineEntryResponse> timeline = new ArrayList<>(
                detailedPostings.stream().map(postingMapper::toResponse).toList());
        Set<UUID> linkedTransfers = detailedPostings.stream()
                .filter(item -> "TRANSFER".equals(item.getSourceEntityType()))
                .map(UserPosting::getSourceEntityId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        Set<UUID> linkedEvents = detailedPostings.stream()
                .filter(item -> "JOINING_RELIEVING".equals(item.getSourceEntityType()))
                .map(UserPosting::getSourceEntityId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        transfers.stream().filter(item -> !linkedTransfers.contains(item.getId()))
                .map(postingMapper::fromTransfer).forEach(timeline::add);
        events.stream().filter(item -> !linkedEvents.contains(item.getId()))
                .map(postingMapper::fromEvent).forEach(timeline::add);
        held.stream().map(charge -> postingMapper.fromCharge(charge, userId)).forEach(timeline::add);
        granted.stream().filter(charge -> !userId.equals(charge.getToUserId()))
                .map(charge -> postingMapper.fromCharge(charge, userId)).forEach(timeline::add);
        timeline.sort(Comparator
                .comparing(PostingTimelineEntryResponse::getEffectiveFrom,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PostingTimelineEntryResponse::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        UserPosting current = currentSubstantivePosting(userId).orElse(null);
        LocalDate retirementDate = employee.dateOfBirth() == null
                ? null : employee.dateOfBirth().plusYears(60);
        String currentStatus = retirementDate != null && !retirementDate.isAfter(LocalDate.now())
                ? "RETIRED" : employee.status();
        String presentPosting = presentPosting(current, employee, departments, sections, designations);

        EmployeeHeaderResponse header = EmployeeHeaderResponse.builder()
                .employeeId(employee.id())
                .profileAttachmentId(employee.profileAttachmentId())
                .employeeName(employee.fullName())
                .employeeCode(employee.employeeCode())
                .designation(designations.get(employee.designationId()))
                .currentDepartment(departments.get(employee.departmentId()))
                .currentSection(sections.get(employee.sectionId()))
                .mobileNumber(employee.mobile())
                .email(employee.email())
                .dateOfBirth(employee.dateOfBirth())
                .dateOfJoining(employee.dateOfJoining())
                .retirementDate(retirementDate)
                .currentStatus(currentStatus)
                .presentPosting(presentPosting)
                .district(employee.district())
                .build();

        return PostingResponse.builder()
                .userId(userId)
                .userName(employee.fullName())
                .employee(header)
                .timeline(timeline)
                .transfers(transfers)
                .joiningRelievingEvents(events)
                .chargesHeld(held)
                .chargesGranted(granted)
                .build();
    }

    @Override
    @Transactional
    public PostingTimelineEntryResponse uploadOrderDocument(UUID postingId, MultipartFile file) {
        UserPosting posting = findPosting(postingId);
        UUID previous = posting.getAttachmentId();
        AttachmentStorageService.StoredAttachment stored = attachmentStorageService.store(
                PostingMapper.ORDER_ENTITY_TYPE, posting.getId(), null, file);
        posting.setAttachmentId(stored.id());
        posting = postingRepository.save(posting);
        if (previous != null && !previous.equals(stored.id())) {
            attachmentStorageService.deactivate(previous);
        }
        auditService.record(MODULE, "USER_POSTING", postingId,
                previous == null ? "ORDER_DOCUMENT_UPLOAD" : "ORDER_DOCUMENT_REPLACE",
                previous == null ? null : previous.toString(), stored.originalFileName());
        return postingMapper.toResponse(posting);
    }

    @Override
    @Transactional
    public PostingTimelineEntryResponse addSupportingDocument(UUID postingId, MultipartFile file) {
        UserPosting posting = findPosting(postingId);
        AttachmentStorageService.StoredAttachment stored = attachmentStorageService.store(
                PostingMapper.SUPPORTING_ENTITY_TYPE, posting.getId(), null, file);
        auditService.record(MODULE, "USER_POSTING", postingId,
                "SUPPORTING_DOCUMENT_UPLOAD", null, stored.originalFileName());
        return postingMapper.toResponse(posting);
    }

    @Override
    @Transactional
    public void recordTransferPosting(TransferHistory transfer,
                                      IdentityFacade.UserRef previousUser) {
        if (postingRepository.findBySourceEntityTypeAndSourceEntityIdAndDeletedFalse(
                "TRANSFER", transfer.getId()).isPresent()) {
            return;
        }
        UserPosting previousPosting = currentSubstantivePosting(transfer.getUserId()).orElse(null);
        relieveCurrentPostingsForTransfer(transfer);
        UserPosting posting = new UserPosting();
        posting.setUserId(transfer.getUserId());
        posting.setFromDepartmentId(transfer.getFromDepartmentId());
        posting.setFromSectionId(transfer.getFromSectionId());
        posting.setFromDesignationId(transfer.getFromDesignationId());
        posting.setDepartmentId(transfer.getToDepartmentId());
        posting.setSectionId(transfer.getToSectionId());
        posting.setDesignationId(transfer.getToDesignationId() != null
                ? transfer.getToDesignationId() : previousUser.designationId());
        posting.setPostingType(transfer.getPostingType() == null
                ? "TRANSFER" : transfer.getPostingType());
        posting.setTransferType(transfer.getTransferType());
        posting.setTransferReason(transfer.getTransferReason());
        posting.setPostingStartDate(transfer.getTransferDate());
        posting.setPostingEndDate(transfer.getEffectiveToDate());
        posting.setOrderNumber(transfer.getOrderNumber());
        posting.setOrderDate(transfer.getOrderDate());
        posting.setGovernmentOrderNumber(transfer.getGovernmentOrderNumber());
        posting.setGovernmentOrderDate(transfer.getGovernmentOrderDate());
        posting.setAttachmentId(transfer.getAttachmentId());
        posting.setFromOfficeName(StringUtils.hasText(transfer.getFromOfficeName())
                ? transfer.getFromOfficeName()
                : previousPosting == null ? null : previousPosting.getOfficeName());
        posting.setFromLocation(StringUtils.hasText(transfer.getFromLocation())
                ? transfer.getFromLocation()
                : previousPosting == null ? null : previousPosting.getLocation());
        posting.setOfficeName(transfer.getToOfficeName());
        posting.setSeatName(transfer.getSeatName());
        posting.setReportingOfficerId(transfer.getReportingOfficerId());
        posting.setControllingOfficerId(transfer.getControllingOfficerId());
        posting.setLocation(transfer.getToLocation());
        posting.setDistrict(transfer.getToDistrict());
        posting.setFromDistrict(StringUtils.hasText(transfer.getFromDistrict())
                ? transfer.getFromDistrict()
                : previousPosting == null ? null : previousPosting.getDistrict());
        posting.setPermanentCharge(Boolean.TRUE);
        posting.setAdditionalCharge(Boolean.FALSE);
        posting.setChargeStatus("ACTIVE");
        posting.setJoiningDate(transfer.getTransferDate());
        posting.setJoiningOrder(transfer.getOrderNumber());
        posting.setJoiningRemarks(transfer.getRemarks());
        posting.setRemarks(transfer.getRemarks());
        posting.setStatus("CURRENT");
        posting.setActive(Boolean.TRUE);
        posting.setSourceEntityType("TRANSFER");
        posting.setSourceEntityId(transfer.getId());
        postingRepository.save(posting);
    }

    @Override
    @Transactional
    public void syncSourceAttachment(String sourceEntityType, UUID sourceEntityId,
                                     UUID attachmentId) {
        postingRepository.findBySourceEntityTypeAndSourceEntityIdAndDeletedFalse(
                        sourceEntityType, sourceEntityId)
                .ifPresent(posting -> {
                    posting.setAttachmentId(attachmentId);
                    postingRepository.save(posting);
                });
    }

    @Override
    @Transactional
    public void applyJoiningRelieving(JoiningRelieving event) {
        UserPosting posting = matchingEventPosting(event)
                .orElseGet(() -> createPostingFromEvent(event));
        if ("JOINING".equals(event.getEventType())) {
            posting.setJoiningDate(event.getEventDate());
            posting.setJoiningTime(event.getEventTime());
            posting.setJoiningOrder(event.getOrderNumber());
            posting.setJoiningRemarks(event.getRemarks());
            posting.setStatus("CURRENT");
            posting.setActive(Boolean.TRUE);
        } else if ("RELIEVING".equals(event.getEventType())) {
            posting.setRelievingDate(event.getEventDate());
            posting.setRelievingTime(event.getEventTime());
            posting.setRelievingOrder(event.getOrderNumber());
            posting.setRelievingRemarks(event.getRemarks());
            posting.setPostingEndDate(event.getEventDate());
            posting.setStatus("RELIEVED");
            posting.setActive(Boolean.FALSE);
        }
        postingRepository.save(posting);
    }

    private Optional<UserPosting> currentSubstantivePosting(UUID userId) {
        return postingRepository
                .findByUserIdAndActiveTrueAndDeletedFalseOrderByPostingStartDateDesc(userId)
                .stream()
                .filter(item -> !Boolean.TRUE.equals(item.getAdditionalCharge()))
                .findFirst();
    }

    private Optional<UserPosting> matchingEventPosting(JoiningRelieving event) {
        if ("JOINING".equals(event.getEventType())) {
            return postingRepository
                    .findByUserIdAndActiveTrueAndDeletedFalseOrderByPostingStartDateDesc(
                            event.getUserId())
                    .stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getAdditionalCharge()))
                    .filter(item -> java.util.Objects.equals(
                            item.getDepartmentId(), event.getDepartmentId()))
                    .filter(item -> java.util.Objects.equals(
                            item.getSectionId(), event.getSectionId()))
                    .filter(item -> java.util.Objects.equals(
                            item.getDesignationId(), event.getDesignationId()))
                    .findFirst();
        }
        List<UserPosting> matches = postingRepository
                .findByUserIdAndDepartmentIdAndSectionIdAndDesignationIdAndDeletedFalseOrderByPostingStartDateDesc(
                        event.getUserId(), event.getDepartmentId(), event.getSectionId(),
                        event.getDesignationId())
                .stream()
                .filter(item -> !Boolean.TRUE.equals(item.getAdditionalCharge()))
                .toList();
        TransferHistory latestTransfer = transferRepository
                .findFirstByUserIdAndDeletedFalseOrderByTransferDateDesc(event.getUserId())
                .orElse(null);
        boolean transferSourceRelieving = latestTransfer != null
                && !event.getEventDate().isBefore(latestTransfer.getTransferDate())
                && java.util.Objects.equals(event.getDepartmentId(), latestTransfer.getFromDepartmentId())
                && java.util.Objects.equals(event.getSectionId(), latestTransfer.getFromSectionId())
                && java.util.Objects.equals(event.getDesignationId(), latestTransfer.getFromDesignationId());
        if (transferSourceRelieving) {
            Optional<UserPosting> previous = matches.stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getActive()))
                    .findFirst();
            if (previous.isPresent()) {
                return previous;
            }
        }
        return matches.stream().filter(item -> Boolean.TRUE.equals(item.getActive())).findFirst()
                .or(() -> matches.stream().findFirst());
    }

    private UserPosting createPostingFromEvent(JoiningRelieving event) {
        IdentityFacade.UserRef user = identityFacade.findUser(event.getUserId())
                .orElseThrow(() -> new ValidationException("Invalid employee"));
        UserPosting posting = new UserPosting();
        posting.setUserId(user.id());
        posting.setDepartmentId(event.getDepartmentId() != null
                ? event.getDepartmentId() : user.departmentId());
        posting.setSectionId(event.getSectionId() != null
                ? event.getSectionId() : user.sectionId());
        posting.setDesignationId(event.getDesignationId() != null
                ? event.getDesignationId() : user.designationId());
        posting.setPostingType(event.getEventType());
        posting.setPostingStartDate(event.getEventDate());
        posting.setOrderNumber(event.getOrderNumber());
        posting.setOrderDate(event.getOrderDate());
        posting.setGovernmentOrderNumber(event.getGovernmentOrderNumber());
        posting.setGovernmentOrderDate(event.getGovernmentOrderDate());
        posting.setAttachmentId(event.getAttachmentId());
        posting.setPermanentCharge(Boolean.TRUE);
        posting.setAdditionalCharge(Boolean.FALSE);
        posting.setStatus("JOINING".equals(event.getEventType()) ? "CURRENT" : "RELIEVED");
        posting.setActive("JOINING".equals(event.getEventType()));
        if ("RELIEVING".equals(event.getEventType())) {
            posting.setPostingEndDate(event.getEventDate());
        }
        posting.setSourceEntityType("JOINING_RELIEVING");
        posting.setSourceEntityId(event.getId());
        return postingRepository.save(posting);
    }

    private void relieveCurrentPostingsForTransfer(TransferHistory transfer) {
        List<UserPosting> active = postingRepository
                .findByUserIdAndActiveTrueAndDeletedFalseOrderByPostingStartDateDesc(
                        transfer.getUserId());
        for (UserPosting posting : active) {
            if (Boolean.TRUE.equals(posting.getAdditionalCharge())) {
                continue;
            }
            LocalDate endDate = transfer.getTransferDate().minusDays(1);
            posting.setPostingEndDate(endDate.isBefore(posting.getPostingStartDate())
                    ? posting.getPostingStartDate() : endDate);
            posting.setRelievingDate(transfer.getTransferDate());
            posting.setRelievingOrder(transfer.getOrderNumber());
            posting.setRelievingRemarks(StringUtils.hasText(transfer.getTransferReason())
                    ? transfer.getTransferReason() : transfer.getRemarks());
            posting.setActive(Boolean.FALSE);
            posting.setStatus("RELIEVED");
            postingRepository.save(posting);
        }
    }

    private void closeActivePostings(UUID userId, LocalDate endDate, String status) {
        List<UserPosting> active = postingRepository
                .findByUserIdAndActiveTrueAndDeletedFalseOrderByPostingStartDateDesc(userId);
        for (UserPosting posting : active) {
            if (Boolean.TRUE.equals(posting.getAdditionalCharge())) {
                continue;
            }
            LocalDate safeEnd = endDate.isBefore(posting.getPostingStartDate())
                    ? posting.getPostingStartDate() : endDate;
            posting.setPostingEndDate(safeEnd);
            posting.setActive(Boolean.FALSE);
            posting.setStatus(status);
            postingRepository.save(posting);
        }
    }

    private void validateDates(CreatePostingRequest request) {
        if (request.getEffectiveTo() != null
                && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new ValidationException("Effective-to date cannot be before effective-from date");
        }
        if (request.getChargeEndDate() != null && request.getChargeStartDate() != null
                && request.getChargeEndDate().isBefore(request.getChargeStartDate())) {
            throw new ValidationException("Charge end date cannot be before charge start date");
        }
        if (request.getRelievingDate() != null && request.getJoiningDate() != null
                && request.getRelievingDate().isBefore(request.getJoiningDate())) {
            throw new ValidationException("Relieving date cannot be before joining date");
        }
    }

    private void validateEmployeeReference(UUID userId, String label) {
        if (userId != null && identityFacade.findUser(userId).isEmpty()) {
            throw new ValidationException("Invalid " + label);
        }
    }

    private void validateOrganization(UUID departmentId, UUID sectionId, UUID designationId) {
        if (departmentId == null || !identityFacade.departmentExists(departmentId)) {
            throw new ValidationException("A valid department is required");
        }
        IdentityFacade.SectionRef section = sectionId == null ? null
                : identityFacade.findSection(sectionId).orElse(null);
        if (section == null || !departmentId.equals(section.departmentId())) {
            throw new ValidationException("A valid section belonging to the department is required");
        }
        if (designationId == null || !identityFacade.designationExists(designationId)) {
            throw new ValidationException("A valid designation is required");
        }
    }

    private UserPosting findPosting(UUID id) {
        return postingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Posting", id));
    }

    private String normalizeType(String value) {
        String type = value == null ? "" : value.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_');
        if (!POSTING_TYPES.contains(type)) {
            throw new ValidationException("Invalid posting type: " + value);
        }
        return type;
    }

    private String statusFor(String postingType, LocalDate effectiveTo) {
        if ("RETIREMENT".equals(postingType)) {
            return "RETIRED";
        }
        if ("RELIEVING".equals(postingType)) {
            return "RELIEVED";
        }
        if ("ADDITIONAL_CHARGE".equals(postingType)) {
            return effectiveTo == null ? "ADDITIONAL_CHARGE" : "PAST";
        }
        return effectiveTo == null ? "CURRENT" : "PAST";
    }

    private String presentPosting(UserPosting current, IdentityFacade.EmployeeRef employee,
                                  Map<UUID, String> departments,
                                  Map<UUID, String> sections,
                                  Map<UUID, String> designations) {
        List<String> parts = new ArrayList<>();
        UUID designationId = current != null ? current.getDesignationId() : employee.designationId();
        UUID departmentId = current != null ? current.getDepartmentId() : employee.departmentId();
        UUID sectionId = current != null ? current.getSectionId() : employee.sectionId();
        add(parts, designations.get(designationId));
        add(parts, departments.get(departmentId));
        add(parts, sections.get(sectionId));
        if (current != null) {
            add(parts, current.getOfficeName());
            add(parts, current.getSeatName());
        }
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private void add(List<String> parts, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(value.trim());
        }
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
