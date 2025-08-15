package com.hinadt.miaocha.infrastructure.email;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Default implementation based on Spring JavaMailSender. */
@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${miaocha.alerts.mail.from:}")
    private String defaultFrom;

    @Value("${miaocha.alerts.mail.enabled:false}")
    private boolean mailEnabled;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendHtml(List<String> to, String subject, String html) {
        if (!mailEnabled) {
            log.info("Mail sending is disabled by configuration. Subject: {}", subject);
            return;
        }
        if (to == null || to.isEmpty()) {
            log.warn("No recipients provided for email: {}", subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            if (defaultFrom != null && !defaultFrom.isBlank()) {
                helper.setFrom(defaultFrom);
            }
            helper.setTo(to.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
