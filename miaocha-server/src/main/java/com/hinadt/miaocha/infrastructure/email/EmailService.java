package com.hinadt.miaocha.infrastructure.email;

import java.util.List;

/** Email sending service abstraction. */
public interface EmailService {
    /**
     * Send an HTML email to the given recipients.
     *
     * @param to list of recipient email addresses
     * @param subject subject line
     * @param html html body content
     */
    void sendHtml(List<String> to, String subject, String html);
}
