package com.investwise.user.controller;

import com.investwise.user.common.ApiResponse;
import com.investwise.user.common.PageResponse;
import com.investwise.user.dto.Requests;
import com.investwise.user.dto.Responses;
import com.investwise.user.model.Enums;
import com.investwise.user.security.AuthUser;
import com.investwise.user.service.ContactService;
import com.investwise.user.service.NotificationService;
import com.investwise.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * User administration.
 * <p>
 * Paths are grouped under the segments the API gateway routes to this service:
 * {@code /admin/users}, {@code /admin/contact-messages} and {@code /admin/user-stats}.
 * Everything else under {@code /api/v1/admin} belongs to the Investment Service.
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "5. Administration")
public class AdminController {

    private final UserService userService;
    private final ContactService contactService;
    private final NotificationService notifications;

    // ---------------- users ----------------

    @GetMapping("/users")
    @Operation(summary = "Search users with pagination and filters")
    public ResponseEntity<ApiResponse<PageResponse<Responses.UserView>>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Enums.Status status,
            @RequestParam(required = false) Enums.Tier tier,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(ApiResponse.ok(userService.search(keyword, status, tier, page, size)));
    }

    @GetMapping("/users/top-investors")
    @Operation(summary = "Highest value active investors")
    public ResponseEntity<ApiResponse<List<Responses.UserView>>> topInvestors(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(userService.topInvestors(limit)));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Fetch any user")
    public ResponseEntity<ApiResponse<Responses.UserView>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.get(id)));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Activate, suspend or lock an account")
    public ResponseEntity<ApiResponse<Responses.UserView>> updateStatus(
            @PathVariable Long id, @Valid @RequestBody Requests.UpdateStatus request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateStatus(id, request),
                "Account status set to " + request.status()));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Permanently delete a user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("User deleted"));
    }

    @PostMapping("/users/announcements")
    @Operation(summary = "Broadcast an in-app announcement",
            description = "Delivered asynchronously; returns as soon as the job is accepted.")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> broadcast(
            @RequestParam @NotBlank String title,
            @RequestParam @NotBlank String message,
            @RequestParam(defaultValue = "INFO") Enums.NotificationType type,
            @RequestBody List<Long> userIds) {
        notifications.broadcast(userIds, title, message, type);
        return ResponseEntity.accepted().body(ApiResponse.ok(
                Map.of("recipients", userIds.size()), "Announcement queued for delivery"));
    }

    // ---------------- statistics ----------------

    @GetMapping("/user-stats")
    @Operation(summary = "Aggregate user figures for the administrator dashboard")
    public ResponseEntity<ApiResponse<Responses.UserStats>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(userService.stats()));
    }

    // ---------------- contact enquiries ----------------

    @GetMapping("/contact-messages")
    @Operation(summary = "List enquiries, optionally filtered")
    public ResponseEntity<ApiResponse<PageResponse<Responses.ContactView>>> listMessages(
            @RequestParam(required = false) Enums.ContactStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(ApiResponse.ok(contactService.list(status, keyword, page, size)));
    }

    @PostMapping("/contact-messages/{id}/reply")
    @Operation(summary = "Answer an enquiry and set its status")
    public ResponseEntity<ApiResponse<Responses.ContactView>> reply(
            @PathVariable String id,
            @Valid @RequestBody Requests.AdminReply request,
            @AuthenticationPrincipal AuthUser admin) {
        return ResponseEntity.ok(ApiResponse.ok(
                contactService.reply(id, request, admin.email()), "Reply recorded"));
    }

    @DeleteMapping("/contact-messages/{id}")
    @Operation(summary = "Delete an enquiry")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable String id) {
        contactService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Enquiry deleted"));
    }
}
