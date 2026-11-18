package com.backoffice.pos.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;

/** Sends transactional emails via the configured SMTP server (e.g. Gmail). */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${app.mail.from:}") String from,
                        @Value("${app.mail.from-name:Smart Flow}") String fromName) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.from = from;
        this.fromName = fromName;
    }

    /** True only when SMTP + a "from" address are configured. */
    public boolean isEnabled() {
        return mailSender != null && StringUtils.hasText(from);
    }

    /** Sent on a background thread — never blocks the HTTP request. */
    @Async
    public void sendPasswordResetOtp(String to, String code, int ttlMinutes) {
        send(to, "Your Smart Flow password reset code",
                otpHtml("Use this code to reset your password.", code, ttlMinutes),
                "password reset");
    }

    /** Email-verification code sent during signup, before the account is created. */
    @Async
    public void sendSignupOtp(String to, String code, int ttlMinutes) {
        send(to, "Verify your email for Smart Flow",
                otpHtml("Use this code to verify your email and finish creating your account.", code, ttlMinutes),
                "signup verification");
    }

    /** Welcome email sent right after an account is created. */
    @Async
    public void sendWelcome(String to, String name) {
        send(to, "Welcome to Smart Flow", welcomeHtml(name), "welcome");
    }

    // ---- internals ----

    private void send(String to, String subject, String html, String kind) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED, "UTF-8");
            try {
                helper.setFrom(from, fromName);
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(from);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent {} email to {}", kind, to);
        } catch (Exception e) {
            log.error("Failed to send {} email to {}: {}", kind, to, e.getMessage());
        }
    }

    private String otpHtml(String intro, String code, int ttlMinutes) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#0f172a">
                  <h2 style="color:#131f78;margin:0 0 8px">Smart Flow</h2>
                  <p style="margin:0 0 16px;color:#64748b">%s</p>
                  <div style="font-size:34px;font-weight:700;letter-spacing:8px;background:#eef0fb;color:#131f78;
                              text-align:center;padding:18px;border-radius:12px">%s</div>
                  <p style="margin:16px 0 0;color:#64748b;font-size:13px">
                    This code expires in %d minutes. If you didn't request it, you can safely ignore this email.
                  </p>
                </div>
                """.formatted(intro, code, ttlMinutes);
    }

    private String welcomeHtml(String name) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#0f172a">
                  <h2 style="color:#131f78;margin:0 0 8px">Welcome to Smart Flow, %s!</h2>
                  <p style="margin:0 0 16px;color:#334155">
                    Thank you for signing up at the Smart Flow application. Your account is ready —
                    sign in to set up your first store and start selling.
                  </p>
                  <p style="margin:16px 0 0;color:#64748b;font-size:13px">
                    — The Smart Flow team (Rubsal Technologies)
                  </p>
                </div>
                """.formatted(name == null || name.isBlank() ? "there" : name);
    }
}
