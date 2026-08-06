package com.investwise.investment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
/**
 * InvestWise-Lite :: Investment Service.
 * Goals, risk profiling, recommendations, portfolios, subscriptions, payments,
 * reports and the educational library.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.investwise.investment.repository.jpa")
@EnableMongoRepositories(basePackages = "com.investwise.investment.repository.mongo")
public class InvestmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvestmentServiceApplication.class, args);
    }
}
