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
import java.time.Year;

/**
 * Sends transactional emails via the configured SMTP server (e.g. Gmail). All
 * emails share one responsive, email-client-safe template (table layout, inline
 * styles, bulletproof button) for a consistent, professional look.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    // Brand palette.
    private static final String BRAND = "#131f78";
    private static final String INK = "#0f172a";
    private static final String BODY = "#475569";
    private static final String MUTED = "#94a3b8";
    private static final String FONT = "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif";

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;
    private final String webBaseUrl;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${app.mail.from:}") String from,
                        @Value("${app.mail.from-name:Smart Flow}") String fromName,
                        @Value("${app.web.base-url:}") String webBaseUrl) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.from = from;
        this.fromName = fromName;
        this.webBaseUrl = webBaseUrl;
    }

    /** True only when SMTP + a "from" address are configured. */
    public boolean isEnabled() {
        return mailSender != null && StringUtils.hasText(from);
    }

    /** Password-reset link sent on a background thread — never blocks the request. */
    @Async
    public void sendPasswordResetLink(String to, String link, int ttlMinutes) {
        String inner = heading("Reset your password")
                + paragraph("We received a request to reset the password for your Smart Flow account. "
                        + "Click the button below to choose a new one.")
                + button(link, "Reset password")
                + expiryNote(ttlMinutes, "If you didn't request this, you can safely ignore this email — your password won't change.")
                + fallback(link);
        send(to, "Reset your Smart Flow password",
                shell("Reset your Smart Flow password — this link expires soon.", inner), "password reset");
    }

    /** Email-verification link sent during signup, before the account is created. */
    @Async
    public void sendSignupVerifyLink(String to, String link, int ttlMinutes) {
        String inner = heading("Confirm your email address")
                + paragraph("Welcome to Smart Flow! You're one step away from your account. "
                        + "Please confirm your email address to activate it and get started.")
                + button(link, "Verify email address")
                + expiryNote(ttlMinutes, "If you didn't create a Smart Flow account, you can safely ignore this email.")
                + fallback(link);
        send(to, "Verify your email for Smart Flow",
                shell("Confirm your email to activate your Smart Flow account.", inner), "signup verification");
    }

    /** Welcome email sent right after an account is created. */
    @Async
    public void sendWelcome(String to, String name) {
        String greeting = StringUtils.hasText(name) ? esc(name) : "there";
        StringBuilder inner = new StringBuilder()
                .append(heading("Welcome to Smart Flow, " + greeting + " 🎉"))
                .append(paragraph("Your account is ready. Smart Flow helps you run your business end to end — "
                        + "set up your stores, manage your catalogue and inventory, and sell from a fast, modern point of sale."))
                .append(paragraph("Sign in to create your first store and start selling."));
        if (StringUtils.hasText(webBaseUrl)) {
            inner.append(button(webBaseUrl + "/business/login", "Sign in to Smart Flow"));
        }
        inner.append(signature());
        send(to, "Welcome to Smart Flow", shell("Your Smart Flow account is ready — sign in to get started.", inner.toString()), "welcome");
    }

    // ---- content pieces ----

    private String heading(String text) {
        return "<h1 style=\"margin:0 0 14px;font-family:" + FONT + ";font-size:22px;line-height:1.3;font-weight:700;color:" + INK + ";\">"
                + esc(text) + "</h1>";
    }

    private String paragraph(String text) {
        return "<p style=\"margin:0 0 18px;font-family:" + FONT + ";font-size:15px;line-height:1.65;color:" + BODY + ";\">"
                + esc(text) + "</p>";
    }

    /** Bulletproof-ish CTA button (centered). */
    private String button(String href, String label) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:8px auto 24px;\">"
                + "<tr><td align=\"center\" bgcolor=\"" + BRAND + "\" style=\"border-radius:12px;\">"
                + "<a href=\"" + href + "\" target=\"_blank\" style=\"display:inline-block;padding:14px 34px;font-family:" + FONT
                + ";font-size:15px;font-weight:600;line-height:1;color:#ffffff;text-decoration:none;border-radius:12px;\">"
                + esc(label) + "</a></td></tr></table>";
    }

    private String expiryNote(int ttlMinutes, String extra) {
        String window = ttlMinutes % 60 == 0 ? (ttlMinutes / 60) + (ttlMinutes == 60 ? " hour" : " hours") : ttlMinutes + " minutes";
        return "<p style=\"margin:0 0 6px;font-family:" + FONT + ";font-size:13px;line-height:1.6;color:" + MUTED + ";\">"
                + "This link expires in " + window + ". " + esc(extra) + "</p>";
    }

    private String fallback(String link) {
        return "<p style=\"margin:14px 0 0;font-family:" + FONT + ";font-size:12px;line-height:1.5;color:" + MUTED + ";word-break:break-all;\">"
                + "If the button doesn't work, copy and paste this link into your browser:<br>"
                + "<a href=\"" + link + "\" target=\"_blank\" style=\"color:" + BRAND + ";\">" + esc(link) + "</a></p>";
    }

    private String signature() {
        return "<p style=\"margin:22px 0 0;font-family:" + FONT + ";font-size:14px;line-height:1.6;color:" + BODY + ";\">"
                + "Warm regards,<br><strong style=\"color:" + INK + ";\">The Smart Flow Team</strong></p>";
    }

    /** Wraps the inner content in the shared, email-safe document shell. */
    private String shell(String preheader, String inner) {
        String template = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <meta http-equiv="X-UA-Compatible" content="IE=edge">
                  <title>Smart Flow</title>
                </head>
                <body style="margin:0;padding:0;background-color:#eef1f6;">
                  <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">{{PREHEADER}}</div>
                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#eef1f6;">
                    <tr><td align="center" style="padding:32px 12px;">
                      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="width:100%;max-width:600px;background-color:#ffffff;border:1px solid #e3e8ef;border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(15,23,42,0.06);">
                        <tr><td style="background-color:{{BRAND}};padding:22px 32px;">
                          <span style="font-family:{{FONT}};font-size:20px;font-weight:700;letter-spacing:0.3px;color:#ffffff;">Smart&nbsp;Flow</span>
                          <span style="font-family:{{FONT}};font-size:12px;color:#c7cbf0;padding-left:8px;">Point of Sale</span>
                        </td></tr>
                        <tr><td style="padding:36px 32px 28px;">{{INNER}}</td></tr>
                        <tr><td style="padding:22px 32px 30px;border-top:1px solid #eef2f7;background-color:#fafbfc;">
                          <p style="margin:0 0 4px;font-family:{{FONT}};font-size:12px;line-height:1.5;color:{{MUTED}};">
                            This is an automated message from Smart Flow — please do not reply to this email.
                          </p>
                          <p style="margin:0;font-family:{{FONT}};font-size:12px;line-height:1.5;color:{{MUTED}};">
                            &copy; {{YEAR}} Rubsal Technologies. All rights reserved.
                          </p>
                        </td></tr>
                      </table>
                      <p style="font-family:{{FONT}};font-size:11px;color:#b6c0cf;margin:16px 0 0;">Smart Flow by Rubsal Technologies</p>
                    </td></tr>
                  </table>
                </body>
                </html>
                """;
        return template
                .replace("{{PREHEADER}}", esc(preheader))
                .replace("{{INNER}}", inner)
                .replace("{{BRAND}}", BRAND)
                .replace("{{FONT}}", FONT)
                .replace("{{MUTED}}", MUTED)
                .replace("{{YEAR}}", String.valueOf(Year.now().getValue()));
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // ---- send ----

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
}
