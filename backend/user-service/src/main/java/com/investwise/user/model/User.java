package com.investwise.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The identity aggregate.
 * <p>
 * Implements {@link Comparable} on registration recency, so a list of users has a
 * sensible default order without every caller supplying a comparator.
 * <p>
 * Timestamps are set by {@code @PrePersist} / {@code @PreUpdate} rather than by
 * Spring Data auditing — two lifecycle callbacks replace a {@code BaseEntity}, an
 * {@code AuditorAware} bean and {@code @EnableJpaAuditing}.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"password", "roles"})
public class User implements Comparable<User> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** BCrypt hash. Never returned by any API and excluded from toString. */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 10)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Enums.Gender gender;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "annual_income", precision = 15, scale = 2)
    private BigDecimal annualIncome;

    @Column(length = 100)
    private String occupation;

    /** One free-text address line replaces the original two-line split. */
    @Column(length = 300)
    private String address;

    @Column(length = 60)
    private String city;

    @Column(length = 60)
    private String state;

    @Column(length = 6)
    private String pincode;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.Status status = Enums.Status.PENDING;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.Tier tier = Enums.Tier.FREE;

    @Builder.Default
    @Column(name = "failed_logins", nullable = false)
    private int failedLogins = 0;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * EAGER because roles are needed on every authenticated request; lazy loading
     * would only add a query. LinkedHashSet keeps the order stable in responses.
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new LinkedHashSet<>();

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ------------------------------------------------------------------

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Integer getAge() {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public boolean isAdmin() {
        return roles.stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
    }

    public void recordSuccessfulLogin() {
        failedLogins = 0;
        lastLoginAt = LocalDateTime.now();
    }

    /** @return true when this failure pushed the account over the lock threshold */
    public boolean recordFailedLogin(int maxAttempts) {
        failedLogins++;
        if (failedLogins >= maxAttempts) {
            status = Enums.Status.LOCKED;
            return true;
        }
        return false;
    }

    /** Newest registration first. */
    @Override
    public int compareTo(User other) {
        return other.createdAt.compareTo(this.createdAt);
    }
}
