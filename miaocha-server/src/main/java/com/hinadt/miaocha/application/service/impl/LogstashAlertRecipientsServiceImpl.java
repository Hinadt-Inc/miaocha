package com.hinadt.miaocha.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hinadt.miaocha.application.service.LogstashAlertRecipientsService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.common.util.UserContextUtil;
import com.hinadt.miaocha.domain.entity.LogstashProcess;
import com.hinadt.miaocha.infrastructure.mapper.LogstashProcessMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** Implementation for managing alert recipients for Logstash processes. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogstashAlertRecipientsServiceImpl implements LogstashAlertRecipientsService {

    private final LogstashProcessMapper logstashProcessMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void updateAlertRecipients(Long processId, List<String> recipients) {
        if (processId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Process ID cannot be null");
        }

        LogstashProcess process = logstashProcessMapper.selectById(processId);
        if (process == null) {
            throw new BusinessException(ErrorCode.LOGSTASH_PROCESS_NOT_FOUND);
        }

        List<String> normalized = normalizeRecipients(recipients);
        String json = toJson(normalized);
        process.setAlertRecipients(json);
        process.setUpdateUser(UserContextUtil.getCurrentUserEmail());

        logstashProcessMapper.update(process);

        log.debug(
                "Updated alert recipients for process [{}], recipients count: {}",
                processId,
                normalized.size());
    }

    private List<String> normalizeRecipients(List<String> recipients) {
        if (CollectionUtils.isEmpty(recipients)) {
            return List.of();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String r : recipients) {
            if (!StringUtils.hasText(r)) continue;
            String trimmed = r.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return new ArrayList<>(set);
    }

    private String toJson(List<String> recipients) {
        try {
            return objectMapper.writeValueAsString(recipients);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR, "Failed to serialize recipients: " + e.getMessage());
        }
    }
}
