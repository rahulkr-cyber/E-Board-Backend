package com.bor.eboard.register.service;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.register.dto.RegisterRow;

import java.time.LocalDate;
import java.util.UUID;

public interface RegisterService {

    /**
     * File register. {@code sectionId} null = central register (all sections).
     * All other filters are optional.
     */
    PageResponse<RegisterRow> register(UUID sectionId, UUID ownerId, UUID priorityId,
                                       String status, LocalDate fromDate, LocalDate toDate,
                                       PaginationRequest pagination);
}
