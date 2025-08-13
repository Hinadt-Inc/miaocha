package com.hinadt.miaocha.application.service;

import java.util.List;

/** Service for managing alert recipients for Logstash processes. */
public interface LogstashAlertRecipientsService {

    /**
     * Update alert recipients for a process.
     *
     * @param processId process ID
     * @param recipients list of recipient emails
     */
    void updateAlertRecipients(Long processId, List<String> recipients);
}
