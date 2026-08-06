package com.investwise.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single entry point for the browser.
 * <p>
 * The gateway does no authentication of its own — each service still validates
 * the JWT itself. Its job is routing and CORS, which means the frontend talks to
 * one origin instead of two and CORS is configured in exactly one place.
 * Routing is declared in {@code application.yml} rather than in Java, so there
 * is no code here to maintain.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
