package com.hina.log.application.service;

import com.hina.log.domain.dto.logstash.LogstashProcessUpdateDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** LogstashProcess元信息更新测试 */
public class LogstashProcessUpdateTest {

    @Test
    public void testLogstashProcessUpdateDTOValidation() {
        // 测试DTO的基本功能
        LogstashProcessUpdateDTO dto = new LogstashProcessUpdateDTO();
        dto.setName("测试进程");
        dto.setModule("test-module");

        // 验证基本的getter和setter功能
        Assertions.assertEquals("测试进程", dto.getName());
        Assertions.assertEquals("test-module", dto.getModule());

        System.out.println("LogstashProcessUpdateDTO测试通过");
    }

    @Test
    public void testEmptyDTO() {
        // 测试空DTO
        LogstashProcessUpdateDTO dto = new LogstashProcessUpdateDTO();
        Assertions.assertNull(dto.getName());
        Assertions.assertNull(dto.getModule());

        System.out.println("空DTO测试通过");
    }
}
