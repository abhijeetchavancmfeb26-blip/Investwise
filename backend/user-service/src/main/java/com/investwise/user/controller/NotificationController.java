package com.investwise.user.controller;

import com.investwise.user.common.ApiResponse;
import com.investwise.user.common.PageResponse;
import com.investwise.user.dto.Responses;
import com.investwise.user.security.AuthUser;
import com.investwise.user.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "4. Notifications")
public class NotificationController {

    private final NotificationService notifications;

    @GetMapping
    @Operation(summary = "Paginated notification feed")
    public ResponseEntity<ApiResponse<PageResponse<Responses.NotificationView>>> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(notifications.list(user.id(), page, size)));
    }

    @GetMapping("/unread")
    @Operation(summary = "Unread notifications")
    public ResponseEntity<ApiResponse<List<Responses.NotificationView>>> unread(
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(notifications.unread(user.id())));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread badge count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", notifications.unreadCount(user.id()))));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark one notification as read")
    public ResponseEntity<ApiResponse<Void>> markRead(@AuthenticationPrincipal AuthUser user,
                                                      @PathVariable String id) {
        notifications.markRead(user.id(), id);
        return ResponseEntity.ok(ApiResponse.message("Marked as read"));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark every notification as read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(
            @AuthenticationPrincipal AuthUser user) {
        int updated = notifications.markAllRead(user.id());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("updated", updated),
                "%d notification(s) marked as read".formatted(updated)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<ApiResponse<Void>> delete(@AuthenticationPrincipal AuthUser user,
                                                    @PathVariable String id) {
        notifications.delete(user.id(), id);
        return ResponseEntity.ok(ApiResponse.message("Notification deleted"));
    }
}
