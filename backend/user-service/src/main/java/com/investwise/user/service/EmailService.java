package com.investwise.user.service;

import com.investwise.user.model.EmailLog;
import com.investwise.user.model.Enums;
import com.investwise.user.model.User;
import com.investwise.user.repository.mongo.EmailLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Outbound email.
 * <p>
 * Simplified considerably: no Thymeleaf templates, no RabbitMQ round trip, no
 * retry sweeper. The HTML is built by one small method, and {@code @Async} on a
 * virtual thread is enough to keep SMTP off the request path Ã¢â‚¬â€ which was the only
 * reason the queue existed.
 * <p>
 * With {@code investwise.mail.enabled=false} (the default) links are written to
 * the log instead, so the platform runs without SMTP credentials.
 */
@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final boolean enabled;
    private final String from;
    private final String frontendUrl;

    public EmailService(JavaMailSender mailSender,
                        EmailLogRepository emailLogRepository,
                        @Value("${investwise.mail.enabled:false}") boolean enabled,
                        @Value("${investwise.mail.from:no-reply@investwise.in}") String from,
                        @Value("${investwise.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.mailSender = mailSender;
        this.emailLogRepository = emailLogRepository;
        this.enabled = enabled;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    public void sendVerification(User user, String token) {
        String link = "http://localhost:8081/api/v1/auth/verify-email?token=" + token;
        send(user.getEmail(), "Verify your InvestWise account", body(user.getFirstName(),
                "Confirm your email address to activate goal planning and personalised recommendations.",
                "Verify my email", link, "This link expires in 24 hours."));
    }

    public void sendPasswordReset(User user, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        send(user.getEmail(), "Reset your InvestWise password", body(user.getFirstName(),
                "We received a request to reset your password. If it wasn't you, ignore this email.",
                "Reset my password", link, "This link expires in 30 minutes and can be used once."));
    }

    public void sendPasswordChanged(User user) {
        send(user.getEmail(), "Your InvestWise password was changed", body(user.getFirstName(),
                "Your password was just changed. Other devices have been signed out.",
                null, null, "If this wasn't you, contact support@investwise.in immediately."));
    }

    public void sendStatusChanged(User user, String status, String reason) {
        send(user.getEmail(), "An update on your InvestWise account", body(user.getFirstName(),
                "The status of your account is now " + status + ".",
                null, null, reason == null || reason.isBlank() ? "" : reason));
    }

    public void sendContactAcknowledgement(String name, String email, String subject) {
        send(email, "We received your message", body(name,
                "Thanks for getting in touch about \"" + subject + "\". "
                        + "A member of the team will respond within one business day.",
                null, null, ""));
    }

    // ------------------------------------------------------------------

    @Async
    public void send(String to, String subject, String html) {
        if (!enabled) {
            log.info("[mail disabled] would send \"{}\" to {}", subject, to);
            record(to, subject, Enums.EmailStatus.SENT, null);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from, "InvestWise");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);

            log.info("Sent \"{}\" to {}", subject, to);
            record(to, subject, Enums.EmailStatus.SENT, null);

        } catch (Exception ex) {
            log.error("Failed to send \"{}\" to {}", subject, to, ex);
            record(to, subject, Enums.EmailStatus.FAILED, ex.getMessage());
        }
    }

    private void record(String to, String subject, Enums.EmailStatus status, String error) {
        try {
            emailLogRepository.save(EmailLog.builder()
                    .recipient(to).subject(subject).status(status).error(error).build());
        } catch (RuntimeException ex) {
            log.warn("Could not write email log for {}", to);
        }
    }

    /** One template method, parameterised. The original had six Thymeleaf files. */
    private String body(String name, String message, String buttonLabel, String link, String footnote) {
        String button = (buttonLabel == null) ? "" : """
                <p style="margin:0 0 24px;">
                  <a href="%s" style="display:inline-block;background:#0f766e;color:#fff;
                     text-decoration:none;padding:12px 28px;border-radius:6px;font-weight:600;">%s</a>
                </p>
                <p style="margin:0 0 8px;color:#64748b;font-size:12px;word-break:break-all;">%s</p>
                """.formatted(link, buttonLabel, link);

        return """
                <div style="font-family:Segoe UI,Arial,sans-serif;background:#f1f5f9;padding:32px;">
                  <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:10px;overflow:hidden;">
                    <div style="background:#0f766e;padding:24px 32px;">
                      <h1 style="margin:0;color:#fff;font-size:20px;">InvestWise</h1>
                    </div>
                    <div style="padding:32px;">
                      <p style="margin:0 0 16px;color:#334155;font-size:15px;">Hello %s,</p>
                      <p style="margin:0 0 24px;color:#334155;font-size:15px;line-height:1.7;">%s</p>
                      %s
                      <p style="margin:0;color:#64748b;font-size:13px;">%s</p>
                    </div>
                    <div style="background:#f8fafc;padding:16px 32px;border-top:1px solid #e2e8f0;">
                      <p style="margin:0;color:#94a3b8;font-size:11px;line-height:1.6;">
                        Automated message from InvestWise. Investments in securities are subject to
                        market risk; read all scheme related documents carefully.
                      </p>
                    </div>
                  </div>
                </div>
                """.formatted(name, message, button, footnote);
    }
}
