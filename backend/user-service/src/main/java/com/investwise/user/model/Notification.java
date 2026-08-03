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

/** In-app notification. Expired automatically after 90 days by a TTL index. */
@Document(collection = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private String title;
    private String message;

    @Builder.Default
    private Enums.NotificationType type = Enums.NotificationType.INFO;

    private String actionUrl;

    @Builder.Default
    @Indexed
    private boolean read = false;

    @Builder.Default
    @Indexed
    private LocalDateTime createdAt = LocalDateTime.now();
}
