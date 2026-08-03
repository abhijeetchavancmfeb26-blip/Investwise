package com.investwise.investment.service;

import com.investwise.investment.model.ActivityLog;
import com.investwise.investment.repository.mongo.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Writes the activity trail. Asynchronous and failure-tolerant, like its User Service twin. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityLogRepository repository;

    @Async
    public void record(Long userId, String email, String action, String description) {
        try {
            repository.save(ActivityLog.builder()
                    .userId(userId).userEmail(email).action(action)
                    .description(description).successful(true).build());
        } catch (RuntimeException ex) {
            log.error("Could not write activity log ({})", action, ex);
        }
    }
}
