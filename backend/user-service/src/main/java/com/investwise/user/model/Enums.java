package com.investwise.user.model;

/**
 * Every enum the User Service needs, in one file.
 * <p>
 * The original spread nine tiny enums across nine files. Grouping them here keeps
 * the domain vocabulary visible at a glance without changing how they are used.
 */
public final class Enums {

    private Enums() { }

    /** Lifecycle of an account. Only ACTIVE may sign in. */
    public enum Status { PENDING, ACTIVE, SUSPENDED, LOCKED }

    public enum Gender { MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY }

    /** Entitlement level. Mirrored from the Investment Service via RabbitMQ. */
    public enum Tier {
        FREE(3), PREMIUM(999), ELITE(999);

        private final int maxGoals;

        Tier(int maxGoals) { this.maxGoals = maxGoals; }

        public int maxGoals() { return maxGoals; }

        public boolean isPremium() { return this != FREE; }
    }

    /** Distinguishes the two kinds of single-use token stored in one table. */
    public enum TokenPurpose {
        VERIFY_EMAIL(24 * 60), RESET_PASSWORD(30);

        private final int validityMinutes;

        TokenPurpose(int validityMinutes) { this.validityMinutes = validityMinutes; }

        public int validityMinutes() { return validityMinutes; }
    }

    public enum ContactStatus { NEW, IN_PROGRESS, RESOLVED, CLOSED }

    public enum NotificationType { INFO, SUCCESS, WARNING, ALERT }

    public enum EmailStatus { SENT, FAILED }
}
