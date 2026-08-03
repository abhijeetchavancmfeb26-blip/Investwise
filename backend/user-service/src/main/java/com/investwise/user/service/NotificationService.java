package com.investwise.user.service;

import com.investwise.user.common.ApiException;
import com.investwise.user.common.PageResponse;
import com.investwise.user.dto.Responses;
import com.investwise.user.model.Enums;
import com.investwise.user.model.Notification;
import com.investwise.user.repository.mongo.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/** The in-app notification feed. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifications;

    public void push(Long userId, String title, String message,
                     Enums.NotificationType type, String actionUrl) {
        notifications.save(Notification.builder()
                .userId(userId).title(title).message(message)
                .type(type == null ? Enums.NotificationType.INFO : type)
                .actionUrl(actionUrl).build());
    }

    /** Platform-wide announcement, written as one bulk insert off the request thread. */
    @Async
    public void broadcast(List<Long> userIds, String title, String message, Enums.NotificationType type) {
        List<Notification> batch = userIds.stream().distinct()
                .map(userId -> Notification.builder()
                        .userId(userId).title(title).message(message)
                        .type(type == null ? Enums.NotificationType.INFO : type).build())
                .toList();

        notifications.saveAll(batch);
        log.info("Broadcast \"{}\" to {} users", title, batch.size());
    }

    public PageResponse<Responses.NotificationView> list(Long userId, int page, int size) {
        return PageResponse.of(
                notifications.findByUserIdOrderByCreatedAtDesc(userId, UserService.pageable(page, size)),
                Responses.NotificationView::from);
    }

    public List<Responses.NotificationView> unread(Long userId) {
        return notifications.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(Responses.NotificationView::from).toList();
    }

    public long unreadCount(Long userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    public void markRead(Long userId, String id) {
        Notification notification = owned(userId, id);
        if (!notification.isRead()) {
            notification.setRead(true);
            notifications.save(notification);
        }
    }

    public int markAllRead(Long userId) {
        List<Notification> unread = notifications.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notifications.saveAll(unread);
        return unread.size();
    }

    public void delete(Long userId, String id) {
        owned(userId, id);
        notifications.deleteById(id);
    }

    /** A notification id alone must never grant access to another user's data. */
    private Notification owned(Long userId, String id) {
        Notification notification = notifications.findById(id)
                .orElseThrow(() -> ApiException.notFound("Notification"));
        if (!notification.getUserId().equals(userId)) {
            throw ApiException.forbidden("That notification belongs to another account");
        }
        return notification;
    }
}
