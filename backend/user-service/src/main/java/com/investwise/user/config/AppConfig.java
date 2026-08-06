package com.investwise.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Async execution and API documentation.
 * <p>
 * The original declared four thread pools with hand-tuned core/max sizes, queue
 * capacities and rejection policies. On Java 21 a single virtual-thread executor
 * does the same job better for this workload — every async task here is I/O bound
 * (SMTP, MongoDB writes), which is exactly what virtual threads are for, and there
 * is no pool to size wrongly.
 */
@Configuration
public class AppConfig {

    @Bean(name = "taskExecutor", destroyMethod = "close")
    public Executor taskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("InvestWise-Lite :: User Service")
                        .version("1.0.0")
                        .description("""
                                Registration, sign-in, profiles, contact enquiries and notifications.

                                Sign in via `POST /api/v1/auth/login`, copy the `accessToken` and paste it
                                into *Authorize*. The same token works on the Investment Service.

                                Demo: `admin@investwise.in / Admin@123`, `rahul.sharma@example.com / User@123`.
                                """))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")));
    }
}
