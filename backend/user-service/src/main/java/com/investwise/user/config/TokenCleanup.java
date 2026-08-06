package com.investwise.user.config;

import com.investwise.user.repository.jpa.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Nightly housekeeping: consumed and expired tokens would otherwise grow without bound. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanup {

    private final TokenRepository tokens;

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void purge() {
        int removed = tokens.purgeStale(LocalDateTime.now().minusDays(7));
        log.info("Token cleanup removed {} stale token(s)", removed);
    }
}
