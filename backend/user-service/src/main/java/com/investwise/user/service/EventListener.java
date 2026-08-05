package com.investwise.user.service;

import com.investwise.user.config.Events;
import com.investwise.user.config.RabbitConfig;
import com.investwise.user.model.Enums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes what the Investment Service publishes.
 * <p>
 * One class with two methods replaces the original's four listener classes. Each
 * method reads from its own queue so the payload type is unambiguous. Handlers are
 * idempotent, because RabbitMQ guarantees at-least-once delivery and a redelivered
 * message must not double-apply.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventListener {

    private final UserService userService;
    private final NotificationService notifications;

    @RabbitListener(queues = RabbitConfig.SUBSCRIPTION_QUEUE)
    public void onSubscriptionChanged(Events.SubscriptionChanged event) {
        log.info("Subscription change for user {} -> {}", event.userId(), event.tier());

        Enums.Tier tier;
        try {
            tier = Enums.Tier.valueOf(event.tier());
        } catch (IllegalArgumentException | NullPointerException ex) {
            tier = Enums.Tier.FREE;
        }

        // Setting the same tier twice is a no-op, which is what makes this safe to redeliver
        userService.syncTier(event.userId(), tier);

        notifications.push(event.userId(),
                tier.isPremium() ? "Premium activated" : "Subscription ended",
                tier.isPremium()
                        ? "Your %s plan is live. Advanced analytics and premium reports are unlocked."
                            .formatted(event.planName())
                        : "Your premium plan has ended. You can renew any time from the Pricing page.",
                tier.isPremium() ? Enums.NotificationType.SUCCESS : Enums.NotificationType.WARNING,
                tier.isPremium() ? "/analytics" : "/pricing");
    }

    @RabbitListener(queues = RabbitConfig.NOTIFY_QUEUE)
    public void onNotify(Events.Notify event) {
        Enums.NotificationType type;
        try {
            type = Enums.NotificationType.valueOf(event.type());
        } catch (IllegalArgumentException | NullPointerException ex) {
            type = Enums.NotificationType.INFO;
        }
        notifications.push(event.userId(), event.title(), event.message(), type, event.actionUrl());
    }
}
