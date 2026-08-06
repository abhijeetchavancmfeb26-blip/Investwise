package com.investwise.investment.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Async execution, Razorpay and API documentation.
 * <p>
 * One virtual-thread executor replaces the original's four hand-tuned pools. Every
 * async task here is I/O bound, which is exactly what virtual threads suit, and
 * there is no pool size to get wrong.
 */
@Slf4j
@Configuration
public class AppConfig {

    @Bean(name = "taskExecutor", destroyMethod = "close")
    public Executor taskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * The key secret never leaves the server: the browser receives only the public
     * key id, and signature verification happens here. That separation is the whole
     * security model of the integration.
     */
    @Bean
    public RazorpayClient razorpayClient(@Value("${razorpay.key-id}") String keyId,
                                         @Value("${razorpay.key-secret}") String keySecret)
            throws RazorpayException {
        if (keyId.startsWith("rzp_live")) {
            log.warn("Razorpay is in LIVE mode. Real payments will be captured.");
        }
        return new RazorpayClient(keyId, keySecret);
    }

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("InvestWise-Lite :: Investment Service")
                        .version("1.0.0")
                        .description("""
                                Goals, risk profiling, recommendations, portfolios, subscriptions,
                                payments, reports and the educational library.

                                Obtain a token from the User Service and paste it into *Authorize*.
                                Both services verify the same signing key, so one token works across
                                the platform.
                                """))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")));
    }
}
