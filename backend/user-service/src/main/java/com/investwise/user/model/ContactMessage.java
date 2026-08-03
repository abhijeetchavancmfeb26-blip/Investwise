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

/** Enquiry from the public contact form. Free-form and write-heavy, so it lives in MongoDB. */
@Document(collection = "contact_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessage {

    @Id
    private String id;

    private String name;

    @Indexed
    private String email;

    private String phone;
    private String subject;
    private String message;

    /** Present when the sender happened to be signed in. */
    private Long userId;

    @Builder.Default
    @Indexed
    private Enums.ContactStatus status = Enums.ContactStatus.NEW;

    private String adminReply;
    private String repliedBy;
    private LocalDateTime repliedAt;

    @Builder.Default
    @Indexed
    private LocalDateTime createdAt = LocalDateTime.now();
}
