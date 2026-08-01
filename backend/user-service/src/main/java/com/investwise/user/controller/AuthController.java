package com.investwise.user.controller;

import com.investwise.user.common.ApiResponse;
import com.investwise.user.dto.Requests;
import com.investwise.user.dto.Responses;
import com.investwise.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public authentication endpoints. Bind, delegate, wrap — no logic here. */
@Validated
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "1. Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new investor",
            description = "Creates a PENDING account and emails a verification link valid for 24 hours.")
    public ResponseEntity<ApiResponse<Responses.UserView>> register(
            @Valid @RequestBody Requests.Register request, HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                authService.register(request, http),
                "Registration successful. Check your inbox to verify your email address."));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in and receive a JWT")
    public ResponseEntity<ApiResponse<Responses.AuthView>> login(
            @Valid @RequestBody Requests.Login request, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request, http), "Signed in"));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Confirm an email address using the token from the link")
    public ResponseEntity<ApiResponse<Responses.UserView>> verifyEmail(
            @RequestParam @NotBlank String token) {
        return ResponseEntity.ok(ApiResponse.ok(authService.verifyEmail(token),
                "Email verified. You can now sign in."));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Re-send the verification email")
    public ResponseEntity<ApiResponse<Void>> resend(@RequestParam @Email String email) {
        authService.resendVerification(email);
        return ResponseEntity.ok(ApiResponse.message("A fresh verification link is on its way."));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link",
            description = "Always reports success, so registered addresses cannot be enumerated.")
    public ResponseEntity<ApiResponse<Void>> forgot(@Valid @RequestBody Requests.ForgotPassword request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(ApiResponse.message(
                "If that address is registered, a reset link has been sent to it."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password using a reset token")
    public ResponseEntity<ApiResponse<Void>> reset(@Valid @RequestBody Requests.ResetPassword request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.message(
                "Password updated. Please sign in with your new password."));
    }

    @GetMapping("/check-email")
    @Operation(summary = "Check whether an email address is available",
            description = "Backs the live availability hint on the registration form.")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(@RequestParam @Email String email) {
        boolean available = authService.isEmailAvailable(email);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("available", available)));
    }
}
