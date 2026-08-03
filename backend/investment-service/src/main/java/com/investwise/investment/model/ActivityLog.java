package com.investwise.investment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** Shares the activity_logs collection with the User Service. */
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

    @Indexed(name = "action_1")
    private String action;

    private String description;
    private String ipAddress;

    @Builder.Default
    private boolean successful = true;

    private String oldValue;
    private String newValue;

    @Builder.Default
    @Indexed(name = "createdAt_1", expireAfterSeconds = 31536000)
    private LocalDateTime createdAt = LocalDateTime.now();
}
