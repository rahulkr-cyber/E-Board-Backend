package com.bor.eboard.dashboard.dto;

import java.time.LocalDateTime;

/** Immutable/synthesized SLA timeline event for a workflow task. */
public record SlaTimelineEvent(String eventType, String label, String detail,
                               LocalDateTime eventAt) { }
