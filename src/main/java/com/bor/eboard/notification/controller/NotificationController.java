package com.bor.eboard.notification.controller;

import com.bor.eboard.common.dto.PageResponse;
import com.bor.eboard.common.dto.PaginationRequest;
import com.bor.eboard.common.response.ApiResponse;
import com.bor.eboard.notification.dto.NotificationResponse;
import com.bor.eboard.notification.dto.UnreadCountResponse;
import com.bor.eboard.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Notification APIs (04_API_SPEC.md section 11). All endpoints are
 * self-scoped to the authenticated user.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification feed")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "My notifications")
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ApiResponse<PageResponse<NotificationResponse>> myNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginationRequest pagination = new PaginationRequest(page, size, "createdAt", "desc");
        return ApiResponse.success(notificationService.myNotifications(unreadOnly, pagination));
    }

    @Operation(summary = "Mark a notification read")
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ApiResponse<NotificationResponse> markRead(@PathVariable UUID id) {
        return ApiResponse.success("Notification marked read", notificationService.markRead(id));
    }

    @Operation(summary = "Mark all notifications read")
    @PatchMapping("/read-all")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ApiResponse<Integer> markAllRead() {
        return ApiResponse.success("All notifications marked read",
                notificationService.markAllRead());
    }

    @Operation(summary = "Unread notification count")
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('NOTIFICATION_VIEW')")
    public ApiResponse<UnreadCountResponse> unreadCount() {
        return ApiResponse.success(new UnreadCountResponse(notificationService.unreadCount()));
    }
}
