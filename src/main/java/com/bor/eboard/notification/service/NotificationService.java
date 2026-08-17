package com.bor.eboard.notification.service;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.notification.dto.NotificationResponse;

import java.util.UUID;

/**
 * In-app notification service (02_ARCHITECTURE.md section 14).
 */
public interface NotificationService {

    PageResponse<NotificationResponse> myNotifications(boolean unreadOnly,
                                                       PaginationRequest pagination);

    NotificationResponse markRead(UUID id);

    int markAllRead();

    long unreadCount();
}
