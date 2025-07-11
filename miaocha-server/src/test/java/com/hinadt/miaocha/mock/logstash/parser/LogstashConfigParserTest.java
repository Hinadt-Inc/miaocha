package com.hinadt.miaocha.mock.logstash.parser;

import static org.junit.jupiter.api.Assertions.*;

import com.hinadt.miaocha.application.logstash.parser.LogstashConfigParser;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LogstashConfigParserTest {

    private final LogstashConfigParser parser = new LogstashConfigParser();

    @Test
    void testExtractTableName() {
        // 测试样例配置
        String configContent =
                "input { \n"
                    + "  kafka { \n"
                    + "    bootstrap_servers => \"10.0.20.25:9092,10.0.20.26:9092,10.0.20.27:9092\""
                    + " \n"
                    + "    topics => [\"doris-input-topic\"] \n"
                    + "    group_id => \"hina-log-test-env-consumer-group-1\" \n"
                    + "    codec => \"json\" \n"
                    + "    auto_offset_reset => \"latest\" \n"
                    + "    consumer_threads => 3 \n"
                    + "  } \n"
                    + "} \n"
                    + " \n"
                    + "filter { \n"
                    + "  # 重命名 [log][file][path] 为 [source] \n"
                    + "  mutate { \n"
                    + "    rename => { \"[log][file][path]\" => \"source\" } \n"
                    + "  } \n"
                    + "} \n"
                    + " \n"
                    + "output { \n"
                    + "  doris { \n"
                    + "    http_hosts => [\"http://10.0.19.5:18030\"] \n"
                    + "    user => \"root\" \n"
                    + "    password => \"${DORIS_PASSWORD:root@root123}\" \n"
                    + "    db => \"log_db\" \n"
                    + "    table => \"log_table_test_env\" \n"
                    + "    headers => { \n"
                    + "      \"format\" => \"json\" \n"
                    + "      \"read_json_by_line\" => \"true\" \n"
                    + "      \"load_to_single_tablet\" => \"true\" \n"
                    + "    } \n"
                    + "  } \n"
                    + "}";

        Optional<String> tableName = parser.extractTableName(configContent);
        assertTrue(tableName.isPresent());
        assertEquals("log_table_test_env", tableName.get());
    }

    @Test
    void testExtractTableNameWithNoTable() {
        // 测试没有表名的配置
        String configContent =
                "input { \n"
                        + "  kafka { \n"
                        + "    bootstrap_servers => \"10.0.20.25:9092\" \n"
                        + "    topics => [\"doris-input-topic\"] \n"
                        + "  } \n"
                        + "} \n"
                        + " \n"
                        + "output { \n"
                        + "  doris { \n"
                        + "    http_hosts => [\"http://10.0.19.5:18030\"] \n"
                        + "    user => \"root\" \n"
                        + "    password => \"password\" \n"
                        + "    db => \"log_db\" \n"
                        + "  } \n"
                        + "}";

        Optional<String> tableName = parser.extractTableName(configContent);
        assertFalse(tableName.isPresent());
    }

    @Test
    void testValidateKafkaConfig() {
        // 测试有效的Kafka配置
        String configContent =
                "input { \n"
                        + "  kafka { \n"
                        + "    bootstrap_servers => \"10.0.20.25:9092\" \n"
                        + "    topics => [\"doris-input-topic\"] \n"
                        + "  } \n"
                        + "} \n"
                        + " \n"
                        + "output { \n"
                        + "  doris { \n"
                        + "    table => \"log_table_test_env\" \n"
                        + "  } \n"
                        + "}";

        LogstashConfigParser.ValidationResult result = parser.validateKafkaConfig(configContent);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateKafkaConfigWithNoBootstrapServers() {
        // 测试没有bootstrap_servers的配置
        String configContent =
                "input { \n"
                        + "  kafka { \n"
                        + "    topics => [\"doris-input-topic\"] \n"
                        + "  } \n"
                        + "} \n"
                        + " \n"
                        + "output { \n"
                        + "  doris { \n"
                        + "    table => \"log_table_test_env\" \n"
                        + "  } \n"
                        + "}";

        LogstashConfigParser.ValidationResult result = parser.validateKafkaConfig(configContent);
        assertFalse(result.isValid());
        assertEquals("未找到Kafka bootstrap_servers配置", result.getErrorMessage());
    }

    @Test
    void testValidateKafkaConfigWithNoTopics() {
        // 测试没有topics的配置
        String configContent =
                "input { \n"
                        + "  kafka { \n"
                        + "    bootstrap_servers => \"10.0.20.25:9092\" \n"
                        + "  } \n"
                        + "} \n"
                        + " \n"
                        + "output { \n"
                        + "  doris { \n"
                        + "    table => \"log_table_test_env\" \n"
                        + "  } \n"
                        + "}";

        LogstashConfigParser.ValidationResult result = parser.validateKafkaConfig(configContent);
        assertFalse(result.isValid());
        assertEquals("未找到Kafka topics配置", result.getErrorMessage());
    }

    @Test
    void testValidateDorisOutput() {
        // 测试有效的Doris输出配置
        String configContent =
                "input { \n"
                        + "  kafka { \n"
                        + "    bootstrap_servers => \"10.0.20.25:9092\" \n"
                        + "    topics => [\"doris-input-topic\"] \n"
                        + "  } \n"
                        + "} \n"
                        + " \n"
                        + "output { \n"
                        + "  doris { \n"
                        + "    table => \"log_table_test_env\" \n"
                        + "  } \n"
                        + "}";

        LogstashConfigParser.ValidationResult result = parser.validateDorisOutput(configContent);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateDorisOutputWithNoTable() {
        // 测试没有表名的Doris输出配置
        String configContent =
                "input { \n"
                        + "  kafka { \n"
                        + "    bootstrap_servers => \"10.0.20.25:9092\" \n"
                        + "    topics => [\"doris-input-topic\"] \n"
                        + "  } \n"
                        + "} \n"
                        + " \n"
                        + "output { \n"
                        + "  doris { \n"
                        + "  } \n"
                        + "}";

        LogstashConfigParser.ValidationResult result = parser.validateDorisOutput(configContent);
        assertFalse(result.isValid());
        assertEquals("未找到Doris表名配置", result.getErrorMessage());
    }

    @Test
    void testValidateConfig() {
        // 测试完整有效的配置
        String configContent =
                "input { \n"
                        + "  kafka { \n"
                        + "    bootstrap_servers => \"10.0.20.25:9092\" \n"
                        + "    topics => [\"doris-input-topic\"] \n"
                        + "  } \n"
                        + "} \n"
                        + " \n"
                        + "output { \n"
                        + "  doris { \n"
                        + "    table => \"log_table_test_env\" \n"
                        + "  } \n"
                        + "}";

        LogstashConfigParser.ValidationResult result = parser.validateConfig(configContent);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateKafkaConfigWithMultipleTopics() {
        // 测试多个topics的配置 - 用户提供的真实用例
        String configContent =
                "input {\n"
                    + "    kafka {\n"
                    + "        topics => [ \"logs-k8s\", \"logs-applog\", \"logs-error\"]\n"
                    + "        bootstrap_servers =>"
                    + " \"172.20.52.15:9092,172.20.52.16:9092,172.20.52.17:9092,172.20.52.18:9092\"\n"
                    + "        codec => json\n"
                    + "        max_poll_records => \"20000\"\n"
                    + "        group_id=> \"k8s-log\"\n"
                    + "    }\n"
                    + "}\n"
                    + " \n"
                    + "output {\n"
                    + "  doris {\n"
                    + "    table => \"log_table_test_env\"\n"
                    + "  }\n"
                    + "}";

        LogstashConfigParser.ValidationResult result = parser.validateKafkaConfig(configContent);
        assertTrue(result.isValid(), "多个topics的配置应该通过验证");
    }
}
