package com.bor.eboard.register.service.impl;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.exception.ForbiddenException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.correspondence.facade.CorrespondenceFacade;
import com.bor.eboard.identity.facade.IdentityFacade;
import com.bor.eboard.register.dto.RegisterRow;
import com.bor.eboard.register.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * BCR-03 Part 11: Section and Central File Registers.
 *
 * <p>These are live views over the files rather than a materialised table, so
 * they can never drift from the data they report. Drafts are excluded — an
 * unmarked file is private to its creator and does not belong on a register.
 *
 * <p>Files are read through {@link CorrespondenceFacade}; this module never
 * touches another module's repositories.
 */
@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {

    private final CorrespondenceFacade correspondenceFacade;
    private final IdentityFacade identityFacade;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RegisterRow> register(UUID sectionId, UUID ownerId, UUID priorityId,
                                              String status, LocalDate fromDate, LocalDate toDate,
                                              PaginationRequest pagination) {
        UUID me = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));

        // A section register may only be read for a section the caller belongs
        // to, unless they hold the central-register permission.
        if (sectionId != null && !SecurityUtils.hasAuthority("REGISTER_CENTRAL_VIEW")) {
            IdentityFacade.UserRef caller = identityFacade.findUser(me)
                    .orElseThrow(() -> new UnauthorizedException("Current user not found"));
            if (caller.sectionId() != null && !caller.sectionId().equals(sectionId)) {
                throw new ForbiddenException(
                        "You may only view the register of your own section.");
            }
        }

        Page<CorrespondenceFacade.RegisterFileRow> page = correspondenceFacade.registerRows(
                sectionId, ownerId, priorityId, status, fromDate, toDate,
                pagination.toPageable());

        LocalDate today = LocalDate.now();
        List<RegisterRow> rows = page.getContent().stream()
                .map(r -> RegisterRow.builder()
                        .fileId(r.fileId())
                        .fileNumber(r.fileNumber())
                        .subject(r.subject())
                        .currentHolder(r.currentHolder())
                        .sectionName(r.sectionName())
                        .priorityName(r.priorityName())
                        .status(r.status())
                        .pendingDays(pendingDays(r.openedDate(), r.closedDate(), today))
                        .createdDate(r.openedDate())
                        .build())
                .toList();

        return PageResponse.of(rows, page);
    }

    /** Days a file has been live; a closed file's pendency stops at closure. */
    private Long pendingDays(LocalDate opened, LocalDate closed, LocalDate today) {
        if (opened == null) {
            return 0L;
        }
        LocalDate end = closed != null ? closed : today;
        return ChronoUnit.DAYS.between(opened, end);
    }
}
