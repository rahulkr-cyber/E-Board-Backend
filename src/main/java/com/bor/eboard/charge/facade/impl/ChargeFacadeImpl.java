package com.bor.eboard.charge.facade.impl;

import com.bor.eboard.charge.entity.JoiningRelieving;
import com.bor.eboard.charge.entity.TransferHistory;
import com.bor.eboard.charge.entity.UserPosting;
import com.bor.eboard.charge.facade.ChargeFacade;
import com.bor.eboard.charge.repository.ChargeAssignmentRepository;
import com.bor.eboard.charge.repository.JoiningRelievingRepository;
import com.bor.eboard.charge.repository.TransferHistoryRepository;
import com.bor.eboard.charge.repository.UserPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChargeFacadeImpl implements ChargeFacade {

    private final ChargeAssignmentRepository chargeRepository;
    private final UserPostingRepository postingRepository;
    private final TransferHistoryRepository transferRepository;
    private final JoiningRelievingRepository joiningRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UUID> activeChargeGrantorsFor(UUID toUserId) {
        if (toUserId == null) {
            return List.of();
        }
        return chargeRepository.findActiveGrantorUserIds(toUserId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> currentOfficeNames(Set<UUID> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> offices = new HashMap<>();
        postingRepository.findByDeletedFalse().stream()
                .filter(posting -> employeeIds.contains(posting.getUserId()))
                .filter(posting -> Boolean.TRUE.equals(posting.getActive()))
                .filter(posting -> !Boolean.TRUE.equals(posting.getAdditionalCharge()))
                .filter(posting -> StringUtils.hasText(posting.getOfficeName()))
                .sorted(Comparator.comparing(UserPosting::getPostingStartDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .forEach(posting -> offices.putIfAbsent(
                        posting.getUserId(), posting.getOfficeName()));
        return Map.copyOf(offices);
    }

    @Override
    @Transactional(readOnly = true)
    public OperationalMetrics operationalMetrics(OperationalScope scope, LocalDate today) {
        if (scope == null || (!scope.organization() && scope.ownerId() == null
                && scope.employeeIds().isEmpty() && scope.sectionIds().isEmpty())) {
            return new OperationalMetrics(0, 0, 0, 0, 0);
        }
        LocalDateTime now = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);

        long additionalCharges = chargeRepository.findAll().stream()
                .filter(charge -> !Boolean.TRUE.equals(charge.getDeleted()))
                .filter(charge -> scope.organization()
                        || (scope.ownerId() != null
                        && scope.ownerId().equals(charge.getToUserId()))
                        || (charge.getSectionId() != null
                        && scope.sectionIds().contains(charge.getSectionId())))
                .filter(charge -> "ACTIVE".equals(charge.getStatus()))
                .filter(charge -> "ADDITIONAL".equals(charge.getChargeType())
                        || "TEMPORARY".equals(charge.getChargeType()))
                .filter(charge -> charge.getEffectiveFrom() == null
                        || !charge.getEffectiveFrom().isAfter(endOfDay))
                .filter(charge -> charge.getEffectiveTo() == null
                        || !charge.getEffectiveTo().isBefore(now))
                .count();

        List<UserPosting> postings = postingRepository.findByDeletedFalse().stream()
                .filter(posting -> scope.organization()
                        || (scope.ownerId() != null
                        && scope.ownerId().equals(posting.getUserId()))
                        || (posting.getSectionId() != null
                        && scope.sectionIds().contains(posting.getSectionId())))
                .toList();
        Set<String> knownSeats = new HashSet<>();
        Set<String> occupiedSeats = new HashSet<>();
        for (UserPosting posting : postings) {
            if (!StringUtils.hasText(posting.getSeatName())) {
                continue;
            }
            String key = seatKey(posting);
            knownSeats.add(key);
            if (Boolean.TRUE.equals(posting.getActive())
                    && (posting.getPostingStartDate() == null
                    || !posting.getPostingStartDate().isAfter(today))
                    && (posting.getPostingEndDate() == null
                    || !posting.getPostingEndDate().isBefore(today))) {
                occupiedSeats.add(key);
            }
        }
        long vacantSeats = knownSeats.stream().filter(seat -> !occupiedSeats.contains(seat)).count();

        List<TransferHistory> transfers = transferRepository.findAll().stream()
                .filter(transfer -> !Boolean.TRUE.equals(transfer.getDeleted()))
                .filter(transfer -> inTransferScope(transfer, scope))
                .toList();
        List<JoiningRelieving> events = joiningRepository.findAll().stream()
                .filter(event -> !Boolean.TRUE.equals(event.getDeleted()))
                .toList();

        Map<UUID, TransferHistory> latestJoiningTransfers = latestTransfers(transfers.stream()
                .filter(transfer -> scope.organization() || scope.ownerId() != null
                        || scope.sectionIds().contains(transfer.getToSectionId()))
                .toList());
        Map<UUID, TransferHistory> latestRelievingTransfers = latestTransfers(transfers.stream()
                .filter(transfer -> scope.organization() || scope.ownerId() != null
                        || scope.sectionIds().contains(transfer.getFromSectionId()))
                .toList());
        long joiningPending = latestJoiningTransfers.values().stream()
                .filter(transfer -> !hasEvent(events, postings, transfer, "JOINING"))
                .count();
        long relievingPending = latestRelievingTransfers.values().stream()
                .filter(transfer -> !hasEvent(events, postings, transfer, "RELIEVING"))
                .count();
        long transfersToday = transfers.stream()
                .filter(transfer -> today.equals(transfer.getTransferDate()))
                .count();

        return new OperationalMetrics(additionalCharges, vacantSeats,
                joiningPending, relievingPending, transfersToday);
    }

    private Map<UUID, TransferHistory> latestTransfers(List<TransferHistory> transfers) {
        Map<UUID, TransferHistory> latest = new HashMap<>();
        transfers.stream()
                .sorted(Comparator.comparing(TransferHistory::getTransferDate).reversed())
                .forEach(transfer -> latest.putIfAbsent(transfer.getUserId(), transfer));
        return latest;
    }

    private boolean inTransferScope(TransferHistory transfer, OperationalScope scope) {
        return scope.organization()
                || (scope.ownerId() != null && scope.ownerId().equals(transfer.getUserId()))
                || (transfer.getFromSectionId() != null
                && scope.sectionIds().contains(transfer.getFromSectionId()))
                || (transfer.getToSectionId() != null
                && scope.sectionIds().contains(transfer.getToSectionId()));
    }

    private boolean hasEvent(List<JoiningRelieving> events, List<UserPosting> postings,
                             TransferHistory transfer, String eventType) {
        UUID expectedDepartment = "JOINING".equals(eventType)
                ? transfer.getToDepartmentId() : transfer.getFromDepartmentId();
        UUID expectedSection = "JOINING".equals(eventType)
                ? transfer.getToSectionId() : transfer.getFromSectionId();
        boolean explicitEvent = events.stream()
                .anyMatch(event -> transfer.getUserId().equals(event.getUserId())
                        && eventType.equals(event.getEventType())
                        && !event.getEventDate().isBefore(transfer.getTransferDate())
                        && java.util.Objects.equals(expectedDepartment, event.getDepartmentId())
                        && java.util.Objects.equals(expectedSection, event.getSectionId()));
        if (explicitEvent) {
            return true;
        }
        if ("JOINING".equals(eventType)) {
            return postings.stream().anyMatch(posting ->
                    transfer.getId().equals(posting.getSourceEntityId())
                            && "TRANSFER".equals(posting.getSourceEntityType())
                            && posting.getJoiningDate() != null);
        }
        return postings.stream().anyMatch(posting ->
                transfer.getUserId().equals(posting.getUserId())
                        && java.util.Objects.equals(expectedDepartment, posting.getDepartmentId())
                        && java.util.Objects.equals(expectedSection, posting.getSectionId())
                        && posting.getRelievingDate() != null
                        && !posting.getRelievingDate().isBefore(transfer.getTransferDate()));
    }

    private String seatKey(UserPosting posting) {
        return String.valueOf(posting.getDepartmentId()) + '|'
                + posting.getSectionId() + '|'
                + posting.getSeatName().trim().toLowerCase();
    }
}
