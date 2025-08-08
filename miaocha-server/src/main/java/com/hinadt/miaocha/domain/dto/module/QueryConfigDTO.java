package com.hinadt.miaocha.domain.dto.module;

import com.hinadt.miaocha.domain.validator.ValidTimeField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 查询配置DTO 用于配置模块的查询相关设置，包括时间分析字段和关键词检索字段 */
@Data
@Schema(
        description = "模块查询配置",
        example =
                """
        {
          "timeField": "log_time",
          "keywordFields": [
            {
              "fieldName": "message",
              "searchMethod": "LIKE"
            },
            {
              "fieldName": "level",
              "searchMethod": "MATCH_ALL"
            }
          ],
          "excludeFields": ["sensitive_data", "internal_id"]
        }
        """)
public class QueryConfigDTO {

    @Size(max = 128, message = "时间字段名长度不能超过128个字符")
    @ValidTimeField
    @Schema(
            description = "分析时间字段名，用于指定模块中哪个字段作为时间查询字段",
            example = "log_time",
            nullable = true,
            maxLength = 128)
    private String timeField;

    @Valid
    @Size(max = 20, message = "关键词检索字段配置不能超过20个")
    @Schema(description = "关键词检索字段配置列表，定义哪些字段被用于关键词搜索以及对应的搜索方法", nullable = true)
    private List<KeywordFieldConfigDTO> keywordFields;

    @Valid
    @Size(max = 50, message = "查询排除字段不能超过50个")
    @Schema(description = "查询字段排除列表，定义在日志查询中需要排除展示和查询的字段名", nullable = true)
    private List<
                    @NotBlank(message = "排除字段名不能为空") @Size(max = 128, message = "排除字段名长度不能超过128个字符")
                    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_.\\[\\]'\"]*$", message = "排除字段名格式不正确")
                    String>
            excludeFields;

    /** 关键词字段配置DTO 定义单个字段的关键词检索配置 */
    @Data
    @Schema(
            description = "关键词字段配置，定义单个字段的检索方法",
            example =
                    """
            {
              "fieldName": "message",
              "searchMethod": "LIKE"
            }
            """)
    public static class KeywordFieldConfigDTO {

        @NotBlank(message = "字段名不能为空")
        @Size(max = 128, message = "字段名长度不能超过128个字符")
        @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_.\\[\\]'\"]*$", message = "字段名格式不正确")
        @Schema(
                description = "字段名，对应数据表中的列名",
                example = "message",
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 128,
                pattern = "^[a-zA-Z_][a-zA-Z0-9_.\\[\\]'\"]*$")
        private String fieldName;

        @NotBlank(message = "检索方法不能为空")
        @Pattern(
                regexp = "^(LIKE|MATCH_ALL|MATCH_ANY|MATCH_PHRASE)$",
                message = "检索方法必须是: LIKE, MATCH_ALL, MATCH_ANY, MATCH_PHRASE 中的一种")
        @Schema(
                description = "检索方法，定义该字段的搜索匹配方式",
                example = "MATCH_PHRASE",
                allowableValues = {"LIKE", "MATCH_ALL", "MATCH_ANY", "MATCH_PHRASE"},
                requiredMode = Schema.RequiredMode.REQUIRED,
                implementation = String.class)
        private String searchMethod;
    }
}
