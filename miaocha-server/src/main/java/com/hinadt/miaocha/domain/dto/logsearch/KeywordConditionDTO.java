package com.hinadt.miaocha.domain.dto.logsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/**
 * 关键字查询条件DTO
 *
 * <p>用于新的结构化关键字查询，支持： - 多个字段名（必须在模块配置的允许字段列表中） - 复杂表达式（支持 && 和 || 运算符） -
 * 使用配置驱动的搜索方法，多个字段按照配置好的查询检索方法拼接SQL，字段间以OR连接，多个条件间以AND连接
 */
@Data
@Schema(description = "关键字查询条件")
public class KeywordConditionDTO {

    @NotEmpty(message = "字段名列表不能为空")
    @Schema(
            description = "字段名列表（每个字段都必须在模块配置的允许字段列表中）",
            example = "[\"message\", \"level\"]",
            required = true)
    private List<String> fieldNames;

    @NotBlank(message = "搜索值不能为空")
    @Schema(
            description =
                    "搜索表达式值，支持复杂表达式，如：\n"
                            + "- 简单关键字：error\n"
                            + "- OR表达式：'error' || 'warning'\n"
                            + "- AND表达式：'error' && 'critical'\n"
                            + "- 复杂嵌套：('error' || 'warning') && 'critical'\n"
                            + "注意：此搜索值将应用到所有选定的字段上，多个字段之间使用OR连接，多个条件之间使用AND连接",
            example = "error")
    private String searchValue;
}
