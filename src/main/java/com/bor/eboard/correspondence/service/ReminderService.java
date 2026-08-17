package com.bor.eboard.correspondence.service;

import com.bor.eboard.correspondence.dto.CreateReminderRequest;
import com.bor.eboard.correspondence.dto.ReminderResponse;
import com.bor.eboard.correspondence.dto.UpdateReminderStatusRequest;

import java.util.List;
import java.util.UUID;

/**
 * Reminder service (03_DATABASE.md 8.5).
 */
public interface ReminderService {

    ReminderResponse create(UUID fileId, CreateReminderRequest request);

    List<ReminderResponse> listByFile(UUID fileId);

    ReminderResponse updateStatus(UUID id, UpdateReminderStatusRequest request);
}
