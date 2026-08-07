package com.backoffice.pos.mail;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;

/** Sends transactional emails via the configured SMTP server (e.g. Gmail). */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${app.mail.from:}") String from,
                        @Value("${app.mail.from-name:RUBSAL POS}") String fromName) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.from = from;
        this.fromName = fromName;
    }

    /** True only when SMTP + a "from" address are configured. */
    public boolean isEnabled() {
        return mailSender != null && StringUtils.hasText(from);
    }

    public void sendPasswordResetOtp(String to, String code, int ttlMinutes) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED, "UTF-8");
        try {
            helper.setFrom(from, fromName);
        } catch (UnsupportedEncodingException e) {
            helper.setFrom(from);
        }
        helper.setTo(to);
        helper.setSubject("Your RUBSAL POS password reset code");
        helper.setText(otpHtml(code, ttlMinutes), true);
        mailSender.send(message);
    }

    private String otpHtml(String code, int ttlMinutes) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;padding:24px;color:#0f172a">
                  <h2 style="color:#131f78;margin:0 0 8px">RUBSAL POS</h2>
                  <p style="margin:0 0 16px;color:#64748b">Use this code to reset your password.</p>
                  <div style="font-size:34px;font-weight:700;letter-spacing:8px;background:#eef0fb;color:#131f78;
                              text-align:center;padding:18px;border-radius:12px">%s</div>
                  <p style="margin:16px 0 0;color:#64748b;font-size:13px">
                    This code expires in %d minutes. If you didn't request it, you can safely ignore this email.
                  </p>
                </div>
                """.formatted(code, ttlMinutes);
    }
}
