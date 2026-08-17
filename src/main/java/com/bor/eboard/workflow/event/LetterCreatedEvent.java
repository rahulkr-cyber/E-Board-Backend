package com.bor.eboard.workflow.event;

import java.util.UUID;

public record LetterCreatedEvent(
        UUID letterId,
        String remarks
) {}