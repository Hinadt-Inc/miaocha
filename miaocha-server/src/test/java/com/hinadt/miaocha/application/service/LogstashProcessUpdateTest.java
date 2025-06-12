package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.dto.logstash.LogstashProcessUpdateDTO;
import io.qameta.allure.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * LogstashProcess元信息更新测试
 *
 * <p>测试秒查系统中Logstash进程元信息更新DTO的功能 验证进程信息的正确设置和获取
 */
@Epic("秒查日志管理系统")
@Feature("Logstash进程管理")
@Story("进程信息更新")
@DisplayName("Logstash进程更新测试")
@Owner("开发团队")
public class LogstashProcessUpdateTest {

    @Test
    @DisplayName("LogstashProcessUpdateDTO验证测试")
    @Description("验证Logstash进程更新DTO的基本功能，包括字段设置和获取")
    @Severity(SeverityLevel.NORMAL)
    public void testLogstashProcessUpdateDTOValidation() {
        Allure.step(
                "创建并设置进程更新DTO",
                () -> {
                    LogstashProcessUpdateDTO dto = new LogstashProcessUpdateDTO();
                    dto.setName("测试进程");
                    dto.setModuleId(1L);

                    Allure.parameter("进程名称", "测试进程");
                    Allure.parameter("模块ID", "1");

                    return dto;
                });

        LogstashProcessUpdateDTO dto = new LogstashProcessUpdateDTO();
        dto.setName("测试进程");
        dto.setModuleId(1L);

        Allure.step(
                "验证DTO字段设置正确性",
                () -> {
                    // 验证基本的getter和setter功能
                    Assertions.assertEquals("测试进程", dto.getName());
                    Assertions.assertEquals(1L, dto.getModuleId());

                    Allure.attachment(
                            "验证结果", "名称: " + dto.getName() + ", 模块ID: " + dto.getModuleId());
                });

        Allure.step(
                "测试完成",
                () -> {
                    System.out.println("LogstashProcessUpdateDTO测试通过");
                });
    }

    @Test
    @DisplayName("空DTO测试")
    @Description("验证空的Logstash进程更新DTO的初始状态")
    @Severity(SeverityLevel.MINOR)
    public void testEmptyDTO() {
        LogstashProcessUpdateDTO dto =
                Allure.step(
                        "创建空的进程更新DTO",
                        () -> {
                            return new LogstashProcessUpdateDTO();
                        });

        Allure.step(
                "验证空DTO的初始状态",
                () -> {
                    // 测试空DTO
                    Assertions.assertNull(dto.getName());
                    Assertions.assertNull(dto.getModuleId());

                    Allure.parameter("进程名称", "null");
                    Allure.parameter("模块ID", "null");
                    Allure.attachment("验证结果", "所有字段均为null，符合预期");
                });

        Allure.step(
                "测试完成",
                () -> {
                    System.out.println("空DTO测试通过");
                });
    }
}
