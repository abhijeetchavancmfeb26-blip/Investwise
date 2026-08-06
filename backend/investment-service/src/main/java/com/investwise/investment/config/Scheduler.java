package com.investwise.investment.config;

import com.investwise.investment.service.GoalService;
import com.investwise.investment.service.PortfolioService;
import com.investwise.investment.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly maintenance.
 * <p>
 * The original ran three concurrent futures with individual exception isolation.
 * These jobs take milliseconds on a database of this size, so running them in
 * sequence inside one try/catch is both simpler and easier to reason about.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Scheduler {

    private final PortfolioService portfolios;
    private final GoalService goals;
    private final SubscriptionService subscriptions;

    @Scheduled(cron = "${investwise.scheduler.cron:0 15 1 * * *}")
    public void nightly() {
        try {
            int revalued = portfolios.refreshMarketValues();
            int refreshed = goals.refreshAllStatuses();
            int expired = subscriptions.expireLapsed();
            log.info("Nightly maintenance: {} holdings revalued, {} goals refreshed, {} subscriptions expired",
                    revalued, refreshed, expired);
        } catch (RuntimeException ex) {
            log.error("Nightly maintenance failed", ex);
        }
    }
}
