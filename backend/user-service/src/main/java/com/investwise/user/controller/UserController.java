package com.investwise.user.controller;

import com.investwise.user.common.ApiResponse;
import com.investwise.user.common.PageResponse;
import com.investwise.user.dto.Requests;
import com.investwise.user.dto.Responses;
import com.investwise.user.security.AuthUser;
import com.investwise.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Self-service endpoints for the signed-in investor. */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "2. User Profile")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Fetch the signed-in user's profile")
    public ResponseEntity<ApiResponse<Responses.UserView>> me(@AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(ApiResponse.ok(userService.get(user.id())));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the signed-in user's profile")
    public ResponseEntity<ApiResponse<Responses.UserView>> update(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody Requests.UpdateProfile request) {
        return ResponseEntity.ok(ApiResponse.ok(
                userService.updateProfile(user.id(), request), "Profile updated"));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody Requests.ChangePassword request) {
        userService.changePassword(user.id(), request);
        return ResponseEntity.ok(ApiResponse.message("Password changed successfully"));
    }

    @GetMapping("/me/activity")
    @Operation(summary = "Paginated activity history")
    public ResponseEntity<ApiResponse<PageResponse<Responses.ActivityView>>> activity(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(ApiResponse.ok(userService.activity(user.id(), page, size)));
    }

    /** A user may read their own record; an administrator may read anyone's. */
    @GetMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.id() or hasRole('ADMIN')")
    @Operation(summary = "Fetch a user by id (self or administrator only)")
    public ResponseEntity<ApiResponse<Responses.UserView>> byId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.get(id)));
    }
}
