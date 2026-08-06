package com.investwise.investment.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * The messages that cross the bus, as records.
 * <p>
 * The original had a {@code BaseEvent} superclass and six subclasses with Lombok
 * annotations. Three records carry the same information; unknown properties are
 * ignored so adding a field to a producer never breaks an older consumer.
 */
public final class Events {

    private Events() { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserRegistered(Long userId, String email, String fullName) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubscriptionChanged(Long userId, String tier, String planName) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Notify(Long userId, String title, String message, String type, String actionUrl) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentCompleted(Long userId, BigDecimal amount, String planName, String invoiceNo) { }
}
