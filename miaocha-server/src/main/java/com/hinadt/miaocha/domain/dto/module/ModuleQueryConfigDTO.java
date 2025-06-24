package com.hinadt.miaocha.domain.dto.module;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 模块查询配置请求DTO 用于PUT接口配置指定模块的查询设置 */
@Data
@Schema(
        description = "模块查询配置请求",
        example =
                """
        {
          "moduleId": 1,
          "queryConfig": {
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
            ]
          }
        }
        """)
public class ModuleQueryConfigDTO {

    @NotNull(message = "模块ID不能为空") @Positive(message = "模块ID必须为正数") @Schema(
            description = "要配置的模块ID，必须是已存在且已完成建表的模块",
            example = "1",
            requiredMode = REQUIRED,
            minimum = "1")
    private Long moduleId;

    @Valid
    @Schema(description = "查询配置信息，包含时间字段和关键词检索字段的配置。如果为null或空，将清空该模块的查询配置", nullable = true)
    private QueryConfigDTO queryConfig;
}
