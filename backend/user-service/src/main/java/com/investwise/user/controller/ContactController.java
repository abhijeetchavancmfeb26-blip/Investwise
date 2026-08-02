package com.investwise.user.controller;

import com.investwise.user.common.ApiResponse;
import com.investwise.user.dto.Requests;
import com.investwise.user.dto.Responses;
import com.investwise.user.security.AuthUser;
import com.investwise.user.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public contact form. Open to anonymous visitors by design. */
@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "3. Contact")
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    @Operation(summary = "Submit a contact enquiry")
    public ResponseEntity<ApiResponse<Responses.ContactView>> submit(
            @Valid @RequestBody Requests.Contact request,
            @AuthenticationPrincipal AuthUser user) {
        // The user id is attached when the sender happens to be signed in
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                contactService.submit(request, user == null ? null : user.id()),
                "Thank you. Our team will respond within one business day."));
    }
}
