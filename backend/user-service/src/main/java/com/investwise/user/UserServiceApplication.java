package com.investwise.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
/**
 * InvestWise-Lite :: User Service.
 * Identity, authentication, profiles, contact enquiries and notifications.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.investwise.user.repository.jpa")
@EnableMongoRepositories(basePackages = "com.investwise.user.repository.mongo")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
