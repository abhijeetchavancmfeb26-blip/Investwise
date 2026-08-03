package com.investwise.user.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * "Who did what, when."
 * <p>
 * This one collection replaces the original's separate audit_logs and
 * activity_logs, which stored the same shape for the same purpose. The optional
 * {@code oldValue} / {@code newValue} fields cover what the audit collection added.
 */
@Document(collection = "activity_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private String userEmail;

    /** e.g. LOGIN, GOAL_CREATED, ACCOUNT_SUSPENDED */
    @Indexed(name = "action_1")
    private String action;

    private String description;
    private String ipAddress;
    private boolean successful;

    /** Populated only when the entry records a change to something. */
    private String oldValue;
    private String newValue;

    @Builder.Default
    @Indexed(name = "createdAt_1", expireAfterSeconds = 31536000)
    private LocalDateTime createdAt = LocalDateTime.now();
}
