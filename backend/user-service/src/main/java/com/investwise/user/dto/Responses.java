package com.investwise.user.dto;

import com.investwise.user.model.ActivityLog;
import com.investwise.user.model.ContactMessage;
import com.investwise.user.model.Enums;
import com.investwise.user.model.Notification;
import com.investwise.user.model.Role;
import com.investwise.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Every outbound payload, as records with a static {@code from(entity)}.
 * <p>
 * This is what removed the mapper classes: the translation lives next to the shape
 * it produces, so there is one place to look rather than two.
 */
public final class Responses {

    private Responses() { }

    public record UserView(Long id, String firstName, String lastName, String fullName, String email,
                           String phone, LocalDate dateOfBirth, Integer age, Enums.Gender gender,
                           String panNumber, BigDecimal annualIncome, String occupation,
                           String address, String city, String state, String pincode,
                           boolean emailVerified, Enums.Status status, Enums.Tier tier, boolean premium,
                           LocalDateTime lastLoginAt, LocalDateTime createdAt, List<String> roles) {

        public static UserView from(User user) {
            return new UserView(
                    user.getId(), user.getFirstName(), user.getLastName(), user.getFullName(),
                    user.getEmail(), user.getPhone(), user.getDateOfBirth(), user.getAge(),
                    user.getGender(), maskPan(user.getPanNumber()), user.getAnnualIncome(),
                    user.getOccupation(), user.getAddress(), user.getCity(), user.getState(),
                    user.getPincode(), user.isEmailVerified(), user.getStatus(), user.getTier(),
                    user.getTier().isPremium(), user.getLastLoginAt(), user.getCreatedAt(),
                    user.getRoles().stream().map(Role::getName).toList());
        }

        /** PAN is sensitive; only the last four characters are ever echoed back. */
        private static String maskPan(String pan) {
            return (pan == null || pan.length() != 10) ? pan : "******" + pan.substring(6);
        }
    }

    public record AuthView(String accessToken, String tokenType, long expiresIn, UserView user) {

        public static AuthView of(String token, long expiresIn, User user) {
            return new AuthView(token, "Bearer", expiresIn, UserView.from(user));
        }
    }

    public record ContactView(String id, String name, String email, String phone, String subject,
                              String message, Long userId, Enums.ContactStatus status,
                              String adminReply, String repliedBy, LocalDateTime repliedAt,
                              LocalDateTime createdAt) {

        public static ContactView from(ContactMessage m) {
            return new ContactView(m.getId(), m.getName(), m.getEmail(), m.getPhone(), m.getSubject(),
                    m.getMessage(), m.getUserId(), m.getStatus(), m.getAdminReply(),
                    m.getRepliedBy(), m.getRepliedAt(), m.getCreatedAt());
        }
    }

    public record NotificationView(String id, String title, String message,
                                   Enums.NotificationType type, String actionUrl,
                                   boolean read, LocalDateTime createdAt) {

        public static NotificationView from(Notification n) {
            return new NotificationView(n.getId(), n.getTitle(), n.getMessage(), n.getType(),
                    n.getActionUrl(), n.isRead(), n.getCreatedAt());
        }
    }

    public record ActivityView(String id, String action, String description, String ipAddress,
                               boolean successful, LocalDateTime createdAt) {

        public static ActivityView from(ActivityLog log) {
            return new ActivityView(log.getId(), log.getAction(), log.getDescription(),
                    log.getIpAddress(), log.isSuccessful(), log.getCreatedAt());
        }
    }

    /** Aggregate figures for the admin dashboard. */
    public record UserStats(long totalUsers, long activeUsers, long pendingVerification,
                            long suspendedUsers, long verifiedUsers, long premiumUsers, long freeUsers,
                            long newUsersLast7Days, long newUsersLast30Days, long loginsLast24Hours,
                            long openContactMessages, Map<String, Long> registrationsByMonth,
                            Map<String, Long> usersByStatus, List<UserView> recentRegistrations) { }
}
