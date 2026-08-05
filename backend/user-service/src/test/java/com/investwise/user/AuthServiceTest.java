package com.investwise.user;

import com.investwise.user.common.ApiException;
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
import com.investwise.user.service.ActivityService;
import com.investwise.user.service.AuthService;
import com.investwise.user.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository users;
    @Mock private RoleRepository roles;
    @Mock private TokenRepository tokens;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private ActivityService activity;
    @Mock private RabbitTemplate rabbit;
    @Mock private HttpServletRequest http;

    private AuthService authService;
    private User active;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, roles, tokens, passwordEncoder,
                jwtService, emailService, activity, rabbit);

        Set<Role> assigned = new LinkedHashSet<>();
        assigned.add(new Role("ROLE_USER"));

        active = User.builder()
                .id(10L).firstName("Rahul").lastName("Sharma")
                .email("rahul.sharma@example.com").password("$2a$12$hashed")
                .phone("9876543210").dateOfBirth(LocalDate.of(1995, 9, 23))
                .emailVerified(true).status(Enums.Status.ACTIVE).tier(Enums.Tier.FREE)
                .roles(assigned).build();
        active.setCreatedAt(LocalDateTime.now());
    }

    private Requests.Register registration() {
        return new Requests.Register("Rahul", "Sharma", "rahul.sharma@example.com",
                "Str0ng@Pass1", "Str0ng@Pass1", "9876543210",
                LocalDate.of(1995, 9, 23), Enums.Gender.MALE, "BCDPA2345G", null, null);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("persists the user, issues a token and publishes the registration event")
        void registers() {
            Requests.Register request = registration();
            when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(users.existsByPhone(anyString())).thenReturn(false);
            when(users.existsByPanNumberIgnoreCase(anyString())).thenReturn(false);
            when(roles.findByName("ROLE_USER")).thenReturn(Optional.of(new Role("ROLE_USER")));
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
            when(users.save(any(User.class))).thenAnswer(inv -> {
                User saved = inv.getArgument(0);
                saved.setId(10L);
                saved.setCreatedAt(LocalDateTime.now());
                return saved;
            });
            when(tokens.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

            Responses.UserView view = authService.register(request, http);

            assertThat(view.email()).isEqualTo("rahul.sharma@example.com");
            assertThat(view.status()).isEqualTo(Enums.Status.PENDING);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(users).save(captor.capture());
            // The raw password must never be persisted
            assertThat(captor.getValue().getPassword()).isNotEqualTo("Str0ng@Pass1");

            verify(emailService).sendVerification(any(User.class), anyString());
            verify(rabbit).convertAndSend(anyString(), anyString(), any(Object.class));
        }

        @Test
        @DisplayName("rejects a duplicate email with 409 before touching anything else")
        void rejectsDuplicateEmail() {
            when(users.existsByEmailIgnoreCase(anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registration(), http))
                    .isInstanceOf(ApiException.class)
                    .extracting(ex -> ((ApiException) ex).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(users, never()).save(any());
        }

        @Test
        @DisplayName("rejects a duplicate phone number")
        void rejectsDuplicatePhone() {
            when(users.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(users.existsByPhone(anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registration(), http))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("mobile number");
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("returns a token for correct credentials")
        void signsIn() {
            var request = new Requests.Login("rahul.sharma@example.com", "User@123", false);
            when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(active));
            when(passwordEncoder.matches("User@123", active.getPassword())).thenReturn(true);
            when(jwtService.generate(active, false)).thenReturn("jwt-token");
            when(jwtService.validitySeconds(false)).thenReturn(28800L);
            when(users.save(any(User.class))).thenReturn(active);

            Responses.AuthView view = authService.login(request, http);

            assertThat(view.accessToken()).isEqualTo("jwt-token");
            assertThat(view.tokenType()).isEqualTo("Bearer");
            assertThat(view.expiresIn()).isEqualTo(28800L);
            assertThat(active.getFailedLogins()).isZero();
            assertThat(active.getLastLoginAt()).isNotNull();
        }

        @Test
        @DisplayName("increments the failure counter on a wrong password")
        void countsFailures() {
            var request = new Requests.Login("rahul.sharma@example.com", "wrong", false);
            when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(active));
            when(passwordEncoder.matches(eq("wrong"), anyString())).thenReturn(false);
            when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> authService.login(request, http))
                    .isInstanceOf(ApiException.class);

            assertThat(active.getFailedLogins()).isEqualTo(1);
            assertThat(active.getStatus()).isEqualTo(Enums.Status.ACTIVE);
        }

        @Test
        @DisplayName("locks the account on the fifth consecutive failure")
        void locksAfterFiveFailures() {
            active.setFailedLogins(4);
            var request = new Requests.Login("rahul.sharma@example.com", "wrong", false);
            when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(active));
            when(passwordEncoder.matches(eq("wrong"), anyString())).thenReturn(false);
            when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> authService.login(request, http))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("locked");

            assertThat(active.getFailedLogins()).isEqualTo(5);
            assertThat(active.getStatus()).isEqualTo(Enums.Status.LOCKED);
            verify(emailService).sendStatusChanged(eq(active), anyString(), anyString());
        }

        @Test
        @DisplayName("refuses an unverified email address")
        void refusesUnverified() {
            active.setEmailVerified(false);
            var request = new Requests.Login("rahul.sharma@example.com", "User@123", false);
            when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(active));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(users.save(any(User.class))).thenReturn(active);

            assertThatThrownBy(() -> authService.login(request, http))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("not been verified");
        }

        @Test
        @DisplayName("gives an unknown email the same error as a wrong password")
        void unknownEmailIsIndistinguishable() {
            var request = new Requests.Login("nobody@example.com", "User@123", false);
            when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request, http))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("Invalid email or password");
        }
    }

    @Nested
    @DisplayName("email verification and password recovery")
    class Recovery {

        @Test
        @DisplayName("activates the account and consumes the token")
        void verifies() {
            active.setEmailVerified(false);
            active.setStatus(Enums.Status.PENDING);
            Token token = Token.issue(active, Enums.TokenPurpose.VERIFY_EMAIL);

            when(tokens.find(anyString(), eq(Enums.TokenPurpose.VERIFY_EMAIL))).thenReturn(Optional.of(token));
            when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(tokens.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

            Responses.UserView view = authService.verifyEmail(token.getToken());

            assertThat(view.emailVerified()).isTrue();
            assertThat(view.status()).isEqualTo(Enums.Status.ACTIVE);
            assertThat(token.isUsed()).isTrue();
        }

        @Test
        @DisplayName("rejects an expired token")
        void rejectsExpired() {
            Token expired = Token.issue(active, Enums.TokenPurpose.VERIFY_EMAIL);
            expired.setExpiresAt(LocalDateTime.now().minusHours(1));
            when(tokens.find(anyString(), any())).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> authService.verifyEmail(expired.getToken()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("stays silent for an unregistered address, so accounts cannot be enumerated")
        void forgotPasswordIsSilent() {
            when(users.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

            authService.forgotPassword("nobody@example.com");

            verify(emailService, never()).sendPasswordReset(any(), anyString());
            verify(tokens, never()).save(any());
        }

        @Test
        @DisplayName("resets the password and unlocks a locked account")
        void resetsPassword() {
            active.setStatus(Enums.Status.LOCKED);
            active.setFailedLogins(5);
            Token token = Token.issue(active, Enums.TokenPurpose.RESET_PASSWORD);

            when(tokens.find(anyString(), eq(Enums.TokenPurpose.RESET_PASSWORD))).thenReturn(Optional.of(token));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$newhash");
            when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(tokens.save(any(Token.class))).thenAnswer(inv -> inv.getArgument(0));

            authService.resetPassword(new Requests.ResetPassword(token.getToken(), "Br@ndNew99", "Br@ndNew99"));

            assertThat(active.getPassword()).isEqualTo("$2a$12$newhash");
            assertThat(active.getStatus()).isEqualTo(Enums.Status.ACTIVE);
            assertThat(active.getFailedLogins()).isZero();
            assertThat(token.isUsed()).isTrue();
            verify(emailService).sendPasswordChanged(active);
        }

        @Test
        @DisplayName("refuses a new password identical to the old one")
        void refusesUnchangedPassword() {
            Token token = Token.issue(active, Enums.TokenPurpose.RESET_PASSWORD);
            when(tokens.find(anyString(), any())).thenReturn(Optional.of(token));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.resetPassword(
                    new Requests.ResetPassword(token.getToken(), "Str0ng@Pass1", "Str0ng@Pass1")))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("must differ");
        }
    }
}
