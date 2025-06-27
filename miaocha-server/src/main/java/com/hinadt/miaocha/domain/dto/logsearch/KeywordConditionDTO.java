package com.hinadt.miaocha.domain.dto.logsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 关键字查询条件DTO
 *
 * <p>用于新的结构化关键字查询，支持： - 指定字段名 - 复杂表达式（支持 && 和 || 运算符） - 配置驱动的搜索方法
 */
@Data
@Schema(description = "关键字查询条件")
public class KeywordConditionDTO {

    @NotBlank(message = "字段名不能为空")
    @Schema(description = "字段名（必须在模块配置的允许字段列表中）", example = "message", required = true)
    private String fieldName;

    @NotBlank(message = "搜索值不能为空")
    @Schema(
            description =
                    "搜索表达式值，支持复杂表达式，如：\n"
                            + "- 简单关键字：error\n"
                            + "- OR表达式：'error' || 'warning'\n"
                            + "- AND表达式：'error' && 'critical'\n"
                            + "- 复杂嵌套：('error' || 'warning') && 'critical'",
            example = "error")
    private String searchValue;

    @Schema(
            description =
                    "搜索方法（可选，优先使用配置中的方法）\n"
                            + "支持：LIKE, MATCH_ALL, MATCH_ANY, MATCH_PHRASE\n"
                            + "如果不指定，使用模块配置中该字段的默认搜索方法",
            allowableValues = {"LIKE", "MATCH_ALL", "MATCH_ANY", "MATCH_PHRASE"})
    private String searchMethod;
}
