package com.bor.eboard.notification.service.impl;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.exception.ResourceNotFoundException;
import com.bor.eboard.common.exception.UnauthorizedException;
import com.bor.eboard.common.util.SecurityUtils;
import com.bor.eboard.notification.dto.NotificationResponse;
import com.bor.eboard.notification.entity.Notification;
import com.bor.eboard.notification.mapper.NotificationMapper;
import com.bor.eboard.notification.repository.NotificationRepository;
import com.bor.eboard.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> myNotifications(boolean unreadOnly,
                                                              PaginationRequest pagination) {
        UUID userId = currentUser();
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByUserIdAndReadFlagFalseOrderByCreatedAtDesc(
                        userId, pagination.toPageable())
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(
                        userId, pagination.toPageable());
        return PageResponse.of(page.getContent().stream().map(mapper::toResponse).toList(), page);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(UUID id) {
        UUID userId = currentUser();
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (!Boolean.TRUE.equals(notification.getReadFlag())) {
            notification.setReadFlag(Boolean.TRUE);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return mapper.toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllRead() {
        return notificationRepository.markAllRead(currentUser(), LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUserIdAndReadFlagFalse(currentUser());
    }

    private UUID currentUser() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
    }
}
