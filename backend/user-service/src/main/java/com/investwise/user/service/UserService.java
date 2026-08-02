package com.investwise.user.service;

import com.investwise.user.common.ApiException;
import com.investwise.user.common.PageResponse;
import com.investwise.user.dto.Requests;
import com.investwise.user.dto.Responses;
import com.investwise.user.model.Enums;
import com.investwise.user.model.User;
import com.investwise.user.repository.mongo.ActivityLogRepository;
import com.investwise.user.repository.mongo.ContactMessageRepository;
import com.investwise.user.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Profile management and user administration.
 * <p>
 * The original split this across a service interface, an implementation and a
 * mapper. Here the translation is a static {@code from()} on the response record,
 * so this class only holds behaviour.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final ActivityLogRepository activityLogs;
    private final ContactMessageRepository contactMessages;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ActivityService activity;

    // ---------------- reads ----------------

    @Transactional(readOnly = true)
    public Responses.UserView get(Long id) {
        return Responses.UserView.from(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<Responses.ActivityView> activity(Long userId, int page, int size) {
        return PageResponse.of(
                activityLogs.findByUserIdOrderByCreatedAtDesc(userId, pageable(page, size)),
                Responses.ActivityView::from);
    }

    // ---------------- profile ----------------

    @Transactional
    public Responses.UserView updateProfile(Long userId, Requests.UpdateProfile request) {
        User user = find(userId);

        users.findByPhone(request.phone())
                .filter(other -> !other.getId().equals(userId))
                .ifPresent(other -> {
                    throw ApiException.conflict("That mobile number belongs to another account");
                });

        String pan = request.normalisedPan();
        if (pan != null && !pan.equalsIgnoreCase(user.getPanNumber())
                && users.existsByPanNumberIgnoreCase(pan)) {
            throw ApiException.conflict("That PAN belongs to another account");
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone().trim());
        // Blank means "leave unchanged" rather than "clear", which is what the form implies
        set(request.dateOfBirth(), user::setDateOfBirth);
        set(request.gender(), user::setGender);
        set(pan, user::setPanNumber);
        set(request.annualIncome(), user::setAnnualIncome);
        set(request.occupation(), user::setOccupation);
        set(request.address(), user::setAddress);
        set(request.city(), user::setCity);
        set(request.state(), user::setState);
        set(request.pincode(), user::setPincode);

        User saved = users.save(user);
        activity.record(userId, saved.getEmail(), "PROFILE_UPDATED", "Profile details updated");
        return Responses.UserView.from(saved);
    }

    private <T> void set(T value, java.util.function.Consumer<T> setter) {
        Optional.ofNullable(value)
                .filter(v -> !(v instanceof String s) || !s.isBlank())
                .ifPresent(setter);
    }

    @Transactional
    public void changePassword(Long userId, Requests.ChangePassword request) {
        User user = find(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw ApiException.badRequest("Your current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        users.save(user);

        emailService.sendPasswordChanged(user);
        activity.record(userId, user.getEmail(), "PASSWORD_CHANGED", "Password changed");
        log.info("Password changed for {}", user.getEmail());
    }

    // ---------------- administration ----------------

    @Transactional(readOnly = true)
    public PageResponse<Responses.UserView> search(String keyword, Enums.Status status, Enums.Tier tier,
                                                   int page, int size) {
        Page<User> result = users.search(
                (keyword == null || keyword.isBlank()) ? null : keyword.trim(),
                status, tier, pageable(page, size));
        return PageResponse.of(result, Responses.UserView::from);
    }

    @Transactional
    public Responses.UserView updateStatus(Long userId, Requests.UpdateStatus request) {
        User user = find(userId);

        if (user.isAdmin() && request.status() != Enums.Status.ACTIVE) {
            throw ApiException.forbidden("An administrator account cannot be suspended");
        }

        Enums.Status previous = user.getStatus();
        user.setStatus(request.status());
        if (request.status() == Enums.Status.ACTIVE) {
            user.setFailedLogins(0);
        }
        User saved = users.save(user);

        emailService.sendStatusChanged(saved, request.status().name(), request.reason());
        activity.recordChange(userId, saved.getEmail(), "STATUS_CHANGED",
                "Account status changed by an administrator", previous.name(), request.status().name());

        log.info("Account {} moved {} -> {}", saved.getEmail(), previous, request.status());
        return Responses.UserView.from(saved);
    }

    @Transactional
    public void delete(Long userId) {
        User user = find(userId);
        if (user.isAdmin()) {
            throw ApiException.forbidden("Administrator accounts cannot be deleted");
        }
        users.delete(user);
        log.warn("User {} deleted", user.getEmail());
    }

    /**
     * Highest-earning active investors.
     * <p>
     * Demonstrates comparator composition inline; the original kept a whole
     * {@code UserComparators} utility class for orderings used in one place each.
     */
    @Transactional(readOnly = true)
    public List<Responses.UserView> topInvestors(int limit) {
        Comparator<User> byValue = Comparator
                .comparing(User::getAnnualIncome, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(User::getLastLoginAt, Comparator.nullsLast(Comparator.reverseOrder()));

        return users.findAll().stream()
                .filter(user -> !user.isAdmin() && user.getStatus() == Enums.Status.ACTIVE)
                .sorted(byValue)
                .limit(Math.clamp(limit, 1, 50))
                .map(Responses.UserView::from)
                .toList();
    }

    /**
     * Dashboard figures.
     * <p>
     * The original fired a dozen concurrent {@code CompletableFuture}s at the same
     * database. These are indexed counts that return in single-digit milliseconds;
     * running them in sequence is simpler to read and no slower in practice.
     */
    @Transactional(readOnly = true)
    public Responses.UserStats stats() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);

        Map<String, Long> byMonth = users.countByMonth(LocalDateTime.now().minusMonths(12)).stream()
                .collect(Collectors.toMap(
                        row -> String.valueOf(row[0]),
                        row -> ((Number) row[1]).longValue(),
                        (a, b) -> a, LinkedHashMap::new));

        Map<String, Long> byStatus = Arrays.stream(Enums.Status.values())
                .collect(Collectors.toMap(Enum::name, users::countByStatus, (a, b) -> a, LinkedHashMap::new));

        return new Responses.UserStats(
                users.count(),
                users.countByStatus(Enums.Status.ACTIVE),
                users.countByStatus(Enums.Status.PENDING),
                users.countByStatus(Enums.Status.SUSPENDED),
                users.countByEmailVerifiedTrue(),
                users.countByTier(Enums.Tier.PREMIUM) + users.countByTier(Enums.Tier.ELITE),
                users.countByTier(Enums.Tier.FREE),
                users.countByCreatedAtAfter(weekAgo),
                users.countByCreatedAtAfter(monthAgo),
                activityLogs.countByActionAndCreatedAtAfter("LOGIN", LocalDateTime.now().minusDays(1)),
                contactMessages.countByStatus(Enums.ContactStatus.NEW)
                        + contactMessages.countByStatus(Enums.ContactStatus.IN_PROGRESS),
                byMonth, byStatus,
                users.findTop5ByOrderByCreatedAtDesc().stream().map(Responses.UserView::from).toList());
    }

    // ---------------- event handling ----------------

    /** Keeps the cached tier in step with the Investment Service, which owns entitlement. */
    @Transactional
    public void syncTier(Long userId, Enums.Tier tier) {
        users.findById(userId).ifPresent(user -> {
            if (user.getTier() != tier) {
                Enums.Tier previous = user.getTier();
                user.setTier(tier);
                users.save(user);
                log.info("User {} tier {} -> {}", user.getEmail(), previous, tier);
            }
        });
    }

    // ---------------- helpers ----------------

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
    }

    /** Clamps the page size, because an unbounded one is a denial-of-service vector. */
    static Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
