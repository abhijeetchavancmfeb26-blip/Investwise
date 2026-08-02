package com.investwise.user.service;

import com.investwise.user.common.ApiException;
import com.investwise.user.common.PageResponse;
import com.investwise.user.dto.Requests;
import com.investwise.user.dto.Responses;
import com.investwise.user.model.ContactMessage;
import com.investwise.user.model.Enums;
import com.investwise.user.repository.mongo.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Public contact enquiries and the administrator's replies. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactMessageRepository messages;
    private final EmailService emailService;
    private final ActivityService activity;

    public Responses.ContactView submit(Requests.Contact request, Long userIdOrNull) {
        ContactMessage saved = messages.save(ContactMessage.builder()
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .phone(request.phone())
                .subject(request.subject().trim())
                .message(request.message().trim())
                .userId(userIdOrNull)
                .status(Enums.ContactStatus.NEW)
                .build());

        emailService.sendContactAcknowledgement(saved.getName(), saved.getEmail(), saved.getSubject());
        activity.record(userIdOrNull, saved.getEmail(), "CONTACT_SUBMITTED", saved.getSubject());

        log.info("Contact enquiry {} from {}", saved.getId(), saved.getEmail());
        return Responses.ContactView.from(saved);
    }

    public PageResponse<Responses.ContactView> list(Enums.ContactStatus status, String keyword,
                                                    int page, int size) {
        var pageable = UserService.pageable(page, size);
        Page<ContactMessage> result = (keyword != null && !keyword.isBlank())
                ? messages.search(keyword.trim(), pageable)
                : status != null ? messages.findByStatus(status, pageable)
                : messages.findAll(pageable);

        return PageResponse.of(result, Responses.ContactView::from);
    }

    public Responses.ContactView reply(String id, Requests.AdminReply request, String adminEmail) {
        ContactMessage message = messages.findById(id)
                .orElseThrow(() -> ApiException.notFound("Enquiry"));

        message.setAdminReply(request.reply());
        message.setStatus(request.status());
        message.setRepliedBy(adminEmail);
        message.setRepliedAt(LocalDateTime.now());

        ContactMessage saved = messages.save(message);
        activity.record(saved.getUserId(), adminEmail, "CONTACT_REPLIED", saved.getSubject());

        log.info("Enquiry {} answered by {}", id, adminEmail);
        return Responses.ContactView.from(saved);
    }

    public void delete(String id) {
        if (!messages.existsById(id)) {
            throw ApiException.notFound("Enquiry");
        }
        messages.deleteById(id);
    }
}
