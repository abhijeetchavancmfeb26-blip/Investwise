package com.investwise.investment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Local mirror of a Razorpay order.
 * <p>
 * {@code userEmail} is denormalised here so the admin payment list can show who
 * paid without a call to the User Service — the simplification that let the whole
 * internal REST channel be deleted.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email", length = 120)
    private String userEmail;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "order_id", nullable = false, unique = true, length = 60)
    private String orderId;

    @Column(name = "payment_id", length = 60)
    private String paymentId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.PaymentStatus status = Enums.PaymentStatus.CREATED;

    @Column(length = 30)
    private String method;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "invoice_no", length = 30)
    private String invoiceNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
