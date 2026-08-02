package com.investwise.user.service;

import com.investwise.user.common.ApiException;
import com.investwise.user.config.Events;
import com.investwise.user.config.RabbitConfig;
import com.investwise.user.dto.Requests;
import com.investwise.user.dto.Responses;
import com.investwise.user.model.Enums;
import com.investwise.user.model.Role;
import com.investwise.user.model.Token;
import com.investwise.user.model.User;
import com.investwise.user.repository.jpa.RoleRepository;
import com.investwise.user.repository.jpa.TokenRepository;
import com.investwise.user.repository.jpa.UserRepository;
import com.investwise.user.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Registration, sign-in and credential recovery.
 * <p>
 * A concrete class rather than an interface plus an {@code Impl}. The split
 * bought nothing here: there was exactly one implementation, and Mockito mocks a
 * class as happily as an interface.
 * <p>
 * Two behaviours are deliberate and worth defending in a viva. Authentication is
 * performed against the {@code PasswordEncoder} directly rather than through
 * {@code AuthenticationManager}, so the failed-attempt counter is updated in the
 * same transaction as the check. And forgot-password always reports success,
 * because responding differently would let anyone enumerate registered addresses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_LOGINS = 5;

    private final UserRepository users;
    private final RoleRepository roles;
    private final TokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final ActivityService activity;
    private final RabbitTemplate rabbit;

    // ------------------------------------------------------------------

    @Transactional
    public Responses.UserView register(Requests.Register request, HttpServletRequest http) {
        String email = request.normalisedEmail();

        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with that email already exists");
        }
        if (users.existsByPhone(request.phone())) {
            throw ApiException.conflict("An account with that mobile number already exists");
        }
        if (request.normalisedPan() != null && users.existsByPanNumberIgnoreCase(request.normalisedPan())) {
            throw ApiException.conflict("An account with that PAN already exists");
        }

        Role userRole = roles.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER has not been seeded"));
        Set<Role> assigned = new LinkedHashSet<>();
        assigned.add(userRole);

        User user = users.save(User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phone(request.phone().trim())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .panNumber(request.normalisedPan())
                .annualIncome(request.annualIncome())
                .occupation(request.occupation())
                .status(Enums.Status.PENDING)
                .roles(assigned)
                .build());

        Token token = tokens.save(Token.issue(user, Enums.TokenPurpose.VERIFY_EMAIL));
        emailService.sendVerification(user, token.getToken());

        // The Investment Service creates the portfolio when it sees this
        rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_USER_REGISTERED,
                new Events.UserRegistered(user.getId(), user.getEmail(), user.getFullName()));

        activity.record(user.getId(), email, "REGISTERED", "Account created", http, true);
        log.info("Registered {}", email);

        return Responses.UserView.from(user);
    }

    // ------------------------------------------------------------------

    @Transactional
    public Responses.AuthView login(Requests.Login request, HttpServletRequest http) {
        String email = request.normalisedEmail();

        User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> {
            activity.record(null, email, "LOGIN_FAILED", "Unknown email", http, false);
            // Identical message to a wrong password, so accounts cannot be enumerated
            return ApiException.unauthorized("Invalid email or password");
        });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            boolean locked = user.recordFailedLogin(MAX_FAILED_LOGINS);
            users.save(user);
            activity.record(user.getId(), email, "LOGIN_FAILED",
                    "Wrong password (attempt " + user.getFailedLogins() + ")", http, false);

            if (locked) {
                log.warn("Locked {} after {} failed attempts", email, user.getFailedLogins());
                emailService.sendStatusChanged(user, "LOCKED",
                        "Your account was locked after repeated failed sign-in attempts.");
                throw ApiException.forbidden("Account locked after too many failed attempts");
            }
            throw ApiException.unauthorized("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            throw ApiException.forbidden("Your email address has not been verified");
        }
        if (user.getStatus() != Enums.Status.ACTIVE) {
            throw ApiException.forbidden("This account is " + user.getStatus().name().toLowerCase());
        }

        user.recordSuccessfulLogin();
        users.save(user);

        activity.record(user.getId(), email, "LOGIN", "Signed in", http, true);
        log.info("{} signed in", email);

        return Responses.AuthView.of(
                jwtService.generate(user, request.rememberMe()),
                jwtService.validitySeconds(request.rememberMe()),
                user);
    }

    // ------------------------------------------------------------------

    @Transactional
    public Responses.UserView verifyEmail(String rawToken) {
        Token token = tokens.find(rawToken, Enums.TokenPurpose.VERIFY_EMAIL)
                .orElseThrow(() -> ApiException.unauthorized("This verification link is not valid"));

        if (!token.isUsable()) {
            throw ApiException.unauthorized("This link has expired or has already been used");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        user.setStatus(Enums.Status.ACTIVE);
        token.setUsed(true);
        users.save(user);
        tokens.save(token);

        activity.record(user.getId(), user.getEmail(), "EMAIL_VERIFIED", "Email address confirmed");
        log.info("Email verified for {}", user.getEmail());

        return Responses.UserView.from(user);
    }

    @Transactional
    public void resendVerification(String email) {
        User user = users.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> ApiException.notFound("Account"));

        if (user.isEmailVerified()) {
            throw ApiException.badRequest("This email address is already verified");
        }
        tokens.invalidateExisting(user.getId(), Enums.TokenPurpose.VERIFY_EMAIL);
        Token token = tokens.save(Token.issue(user, Enums.TokenPurpose.VERIFY_EMAIL));
        emailService.sendVerification(user, token.getToken());
    }

    // ------------------------------------------------------------------

    @Transactional
    public void forgotPassword(String email) {
        users.findByEmailIgnoreCase(email.trim().toLowerCase()).ifPresentOrElse(user -> {
            tokens.invalidateExisting(user.getId(), Enums.TokenPurpose.RESET_PASSWORD);
            Token token = tokens.save(Token.issue(user, Enums.TokenPurpose.RESET_PASSWORD));
            emailService.sendPasswordReset(user, token.getToken());
            activity.record(user.getId(), user.getEmail(), "PASSWORD_RESET_REQUESTED", "Reset link issued");
            log.info("Reset link issued for {}", user.getEmail());
        }, () -> log.info("Reset requested for unregistered address {}", email));
        // Either way the caller is told the same thing.
    }

    @Transactional
    public void resetPassword(Requests.ResetPassword request) {
        Token token = tokens.find(request.token(), Enums.TokenPurpose.RESET_PASSWORD)
                .orElseThrow(() -> ApiException.unauthorized("This reset link is not valid"));

        if (!token.isUsable()) {
            throw ApiException.unauthorized("This link has expired or has already been used");
        }

        User user = token.getUser();
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw ApiException.badRequest("The new password must differ from your current one");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setFailedLogins(0);
        if (user.getStatus() == Enums.Status.LOCKED) {
            user.setStatus(Enums.Status.ACTIVE);
        }
        token.setUsed(true);
        users.save(user);
        tokens.save(token);

        emailService.sendPasswordChanged(user);
        activity.record(user.getId(), user.getEmail(), "PASSWORD_CHANGED", "Password reset completed");
        log.info("Password reset for {}", user.getEmail());
    }

    public boolean isEmailAvailable(String email) {
        return !users.existsByEmailIgnoreCase(email.trim().toLowerCase());
    }
}
