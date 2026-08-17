package com.bor.eboard.dms.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record DmsAuditSearchRequest(
        UUID documentId,
        String entityType,
        String action,
        UUID actorId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        Integer page,
        Integer size) {
}
