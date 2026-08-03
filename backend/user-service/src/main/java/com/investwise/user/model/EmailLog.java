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

/** Delivery record for every outbound email — the audit trail for "I never got the link". */
@Document(collection = "email_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog {

    @Id
    private String id;

    @Indexed
    private String recipient;

    private String subject;

    @Indexed
    private Enums.EmailStatus status;

    private String error;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
