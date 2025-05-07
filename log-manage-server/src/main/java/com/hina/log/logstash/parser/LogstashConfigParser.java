package com.hina.log.logstash.parser;

import com.hina.log.exception.ErrorCode;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logstash配置解析器
 * 用于解析Logstash配置文件，提取关键信息
 */
@Component
public class LogstashConfigParser {
    private static final Logger logger = LoggerFactory.getLogger(LogstashConfigParser.class);

    // 匹配Doris表名的正则表达式 - 直接从全文中提取
    private static final Pattern DORIS_TABLE_PATTERN = Pattern.compile(
            "doris\\s*\\{[\\s\\S]*?table\\s*=>\\s*[\"']([^\"']+)[\"']", Pattern.DOTALL);

    // 匹配Kafka输入部分的正则表达式
    private static final Pattern KAFKA_INPUT_PATTERN = Pattern.compile(
            "input\\s*\\{\\s*kafka\\s*\\{([^}]+)\\}\\s*\\}", Pattern.DOTALL);

    // 匹配Kafka bootstrap_servers的正则表达式
    private static final Pattern KAFKA_BOOTSTRAP_SERVERS_PATTERN = Pattern.compile(
            "bootstrap_servers\\s*=>\\s*[\"']([^\"']+)[\"']", Pattern.DOTALL);

    // 匹配Kafka topics的正则表达式
    private static final Pattern KAFKA_TOPICS_PATTERN = Pattern.compile(
            "topics\\s*=>\\s*\\[\\s*[\"']([^\"']+)[\"']\\s*\\]", Pattern.DOTALL);

    /**
     * 从Logstash配置中提取Doris表名
     *
     * @param configContent Logstash配置内容
     * @return 表名，如果未找到则返回空
     */
    public Optional<String> extractTableName(String configContent) {
        if (!StringUtils.hasText(configContent)) {
            return Optional.empty();
        }

        try {
            // 直接从全文中匹配表名
            Matcher tableNameMatcher = DORIS_TABLE_PATTERN.matcher(configContent);
            if (tableNameMatcher.find()) {
                String tableName = tableNameMatcher.group(1);
                logger.info("从Logstash配置中提取到Doris表名: {}", tableName);
                return Optional.of(tableName);
            }

            logger.warn("未能从Logstash配置中提取到Doris表名");
            return Optional.empty();
        } catch (Exception e) {
            logger.error("解析Logstash配置提取表名时发生错误", e);
            return Optional.empty();
        }
    }

    /**
     * 验证Logstash配置中的Kafka配置是否有效
     *
     * @param configContent Logstash配置内容
     * @return 验证结果，包含是否有效和错误信息
     */
    public ValidationResult validateKafkaConfig(String configContent) {
        if (!StringUtils.hasText(configContent)) {
            return ValidationResult.invalid(ErrorCode.VALIDATION_ERROR, "Logstash配置不能为空");
        }

        try {
            // 匹配Kafka输入部分
            Matcher kafkaInputMatcher = KAFKA_INPUT_PATTERN.matcher(configContent);
            if (!kafkaInputMatcher.find()) {
                return ValidationResult.invalid(ErrorCode.LOGSTASH_CONFIG_KAFKA_MISSING, "未找到Kafka输入配置");
            }

            String kafkaInput = kafkaInputMatcher.group(1);

            // 验证bootstrap_servers
            Matcher bootstrapServersMatcher = KAFKA_BOOTSTRAP_SERVERS_PATTERN.matcher(kafkaInput);
            if (!bootstrapServersMatcher.find()) {
                return ValidationResult.invalid(ErrorCode.LOGSTASH_CONFIG_KAFKA_MISSING, "未找到Kafka bootstrap_servers配置");
            }

            String bootstrapServers = bootstrapServersMatcher.group(1);
            if (!StringUtils.hasText(bootstrapServers)) {
                return ValidationResult.invalid(ErrorCode.LOGSTASH_CONFIG_KAFKA_MISSING, "Kafka bootstrap_servers不能为空");
            }

            // 验证topics
            Matcher topicsMatcher = KAFKA_TOPICS_PATTERN.matcher(kafkaInput);
            if (!topicsMatcher.find()) {
                return ValidationResult.invalid(ErrorCode.LOGSTASH_CONFIG_KAFKA_MISSING, "未找到Kafka topics配置");
            }

            String topics = topicsMatcher.group(1);
            if (!StringUtils.hasText(topics)) {
                return ValidationResult.invalid(ErrorCode.LOGSTASH_CONFIG_KAFKA_MISSING, "Kafka topics不能为空");
            }

            return ValidationResult.valid();
        } catch (Exception e) {
            logger.error("验证Kafka配置时发生错误", e);
            return ValidationResult.invalid(ErrorCode.LOGSTASH_CONFIG_INVALID, "验证Kafka配置时发生错误: " + e.getMessage());
        }
    }

    /**
     * 验证Logstash配置中的Doris输出配置是否有效
     *
     * @param configContent Logstash配置内容
     * @return 验证结果，包含是否有效和错误信息
     */
    public ValidationResult validateDorisOutput(String configContent) {
        if (!StringUtils.hasText(configContent)) {
            return ValidationResult.invalid(ErrorCode.VALIDATION_ERROR, "Logstash配置不能为空");
        }

        try {
            // 首先检查是否有output部分
            if (!configContent.contains("output")) {
                return ValidationResult.invalid(ErrorCode.LOGSTASH_CONFIG_DORIS_MISSING, "未找到output配置");
            }

            // 检查是否有doris输出
            if (!configContent.contains("doris")) {
                return ValidationResult.invalid(ErrorCode.LOGSTASH_CONFIG_DORIS_MISSING, "未找到Doris输出配置");
            }

            // 验证表名
            Matcher tableNameMatcher = DORIS_TABLE_PATTERN.matcher(configContent);
            if (!tableNameMatcher.find()) {
                return ValidationResult.invalid(ErrorCode.LOGSTASH_CONFIG_TABLE_MISSING, "未找到Doris表名配置");
            }

            String tableName = tableNameMatcher.group(1);
            if (!StringUtils.hasText(tableName)) {
                return ValidationResult.invalid(ErrorCode.LOGSTASH_CONFIG_TABLE_MISSING, "Doris表名不能为空");
            }

            return ValidationResult.valid();
        } catch (Exception e) {
            logger.error("验证Doris输出配置时发生错误", e);
            return ValidationResult.invalid(ErrorCode.LOGSTASH_CONFIG_INVALID, "验证Doris输出配置时发生错误: " + e.getMessage());
        }
    }

    /**
     * 验证整个Logstash配置
     *
     * @param configContent Logstash配置内容
     * @return 验证结果，包含是否有效和错误信息
     */
    public ValidationResult validateConfig(String configContent) {
        if (!StringUtils.hasText(configContent)) {
            return ValidationResult.invalid(ErrorCode.VALIDATION_ERROR, "Logstash配置不能为空");
        }

        // 验证Kafka配置
        ValidationResult kafkaResult = validateKafkaConfig(configContent);
        if (!kafkaResult.isValid()) {
            return kafkaResult;
        }

        // 验证Doris输出配置
        ValidationResult dorisResult = validateDorisOutput(configContent);
        if (!dorisResult.isValid()) {
            return dorisResult;
        }

        return ValidationResult.valid();
    }

    /**
     * 验证结果类
     */
    @Getter
    public static class ValidationResult {
        private final boolean valid;
        private final ErrorCode errorCode;
        private final String errorMessage;

        private ValidationResult(boolean valid, ErrorCode errorCode, String errorMessage) {
            this.valid = valid;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult invalid(ErrorCode errorCode, String errorMessage) {
            return new ValidationResult(false, errorCode, errorMessage);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, ErrorCode.VALIDATION_ERROR, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }
    }
}
