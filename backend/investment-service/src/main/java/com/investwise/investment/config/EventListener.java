package com.investwise.investment.config;

import com.investwise.investment.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to user lifecycle events.
 * <p>
 * Creating the portfolio here means the investor's dashboard is never empty
 * because of a missing container, and the User Service never needs to know that
 * portfolios exist. Idempotent, because delivery is at-least-once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventListener {

    private final PortfolioService portfolios;

    @RabbitListener(queues = RabbitConfig.USER_QUEUE)
    public void onUserRegistered(Events.UserRegistered event) {
        log.info("User {} registered; ensuring a portfolio exists", event.userId());
        if (event.userId() != null) {
            portfolios.ensureExists(event.userId());
        }
    }
}
