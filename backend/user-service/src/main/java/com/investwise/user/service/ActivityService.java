package com.investwise.user.service;

import com.investwise.user.model.ActivityLog;
import com.investwise.user.repository.mongo.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Writes the activity trail.
 * <p>
 * One service replaces the original's separate audit and activity services, which
 * wrote the same shape to two collections. Every write is asynchronous and
 * swallows its own failures: logging is a side concern and must never add latency
 * to, or fail, the business transaction it describes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityLogRepository repository;

    @Async
    public void record(Long userId, String email, String action, String description) {
        save(ActivityLog.builder()
                .userId(userId).userEmail(email).action(action)
                .description(description).successful(true).build());
    }

    @Async
    public void record(Long userId, String email, String action, String description,
                       HttpServletRequest request, boolean successful) {
        save(ActivityLog.builder()
                .userId(userId).userEmail(email).action(action).description(description)
                .ipAddress(clientIp(request)).successful(successful).build());
    }

    /** Use when the entry records a change, so before/after are both preserved. */
    @Async
    public void recordChange(Long userId, String email, String action, String description,
                             String oldValue, String newValue) {
        save(ActivityLog.builder()
                .userId(userId).userEmail(email).action(action).description(description)
                .oldValue(oldValue).newValue(newValue).successful(true).build());
    }

    private void save(ActivityLog entry) {
        try {
            repository.save(entry);
        } catch (RuntimeException ex) {
            log.error("Could not write activity log ({})", entry.getAction(), ex);
        }
    }

    /** Proxy-aware, falling back to the socket address. */
    private String clientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded == null || forwarded.isBlank())
                ? request.getRemoteAddr()
                : forwarded.split(",")[0].trim();
    }
}
