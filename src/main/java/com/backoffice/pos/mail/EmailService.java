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

    /** Password-reset link sent on a background thread — never blocks the request. */
    @Async
    public void sendPasswordResetLink(String to, String link, int ttlMinutes) {
        send(to, "Reset your Smart Flow password",
                linkHtml("Reset your password",
                        "We received a request to reset your password. Click the button below to choose a new one.",
                        "Reset password", link, ttlMinutes),
                "password reset");
    }

    /** Email-verification link sent during signup, before the account is created. */
    @Async
    public void sendSignupVerifyLink(String to, String link, int ttlMinutes) {
        send(to, "Verify your email for Smart Flow",
                linkHtml("Verify your email",
                        "Thanks for signing up! Click the button below to verify your email and activate your account.",
                        "Verify email", link, ttlMinutes),
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

    private String linkHtml(String heading, String intro, String button, String link, int ttlMinutes) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#0f172a">
                  <h2 style="color:#131f78;margin:0 0 8px">%s</h2>
                  <p style="margin:0 0 20px;color:#334155">%s</p>
                  <div style="text-align:center;margin:0 0 20px">
                    <a href="%s" style="display:inline-block;background:#131f78;color:#ffffff;text-decoration:none;
                       font-weight:600;padding:14px 28px;border-radius:12px">%s</a>
                  </div>
                  <p style="margin:0 0 6px;color:#64748b;font-size:13px">
                    This link expires in %d minutes. If you didn't request it, you can safely ignore this email.
                  </p>
                  <p style="margin:0;color:#94a3b8;font-size:12px;word-break:break-all">
                    If the button doesn't work, paste this link into your browser:<br>%s
                  </p>
                </div>
                """.formatted(heading, intro, link, button, ttlMinutes, link);
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
