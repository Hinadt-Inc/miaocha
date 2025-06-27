package com.hinadt.miaocha.application.service.sql.builder;

import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.application.service.sql.search.SearchMethod;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.KeywordConditionDTO;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.LogSearchDTODecorator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 关键字条件构建器
 *
 * <p>处理基于keywordConditions的查询条件构建 支持配置驱动的搜索方法（LIKE, MATCH_PHRASE, MATCH_ANY, MATCH_ALL）
 * 支持复杂表达式解析（&& 和 || 运算符，括号嵌套）
 */
@Component
public class KeywordConditionBuilder {

    @Autowired private QueryConfigValidationService queryConfigValidationService;

    /** 构建关键字查询条件 */
    public String buildKeywordConditions(LogSearchDTO dto) {
        if (dto.getKeywordConditions() == null || dto.getKeywordConditions().isEmpty()) {
            return "";
        }

        List<KeywordConditionDTO> keywordConditions = dto.getKeywordConditions();

        // 验证字段权限
        queryConfigValidationService.validateKeywordFieldPermissions(dto, keywordConditions);

        // 获取字段搜索方法映射
        Map<String, String> fieldSearchMethodMap =
                queryConfigValidationService.getFieldSearchMethodMap(dto.getModule());

        StringBuilder condition = new StringBuilder();
        boolean isFirstCondition = true;

        // 处理每个关键字条件
        for (KeywordConditionDTO keywordCondition : keywordConditions) {
            if (StringUtils.isNotBlank(keywordCondition.getFieldName())
                    && StringUtils.isNotBlank(keywordCondition.getSearchValue())) {

                String fieldName = keywordCondition.getFieldName().trim();
                String searchValue = keywordCondition.getSearchValue().trim();

                // 优先使用 KeywordConditionDTO 中指定的搜索方法，如果没有指定则使用配置中的默认方法
                String searchMethodToUse;
                if (StringUtils.isNotBlank(keywordCondition.getSearchMethod())) {
                    // 使用请求中指定的搜索方法
                    searchMethodToUse = keywordCondition.getSearchMethod().trim();
                } else {
                    // 使用配置中的默认搜索方法
                    // 如果是装饰器，需要用原始字段名查找配置
                    String configFieldName = fieldName;
                    if (dto instanceof LogSearchDTODecorator) {
                        configFieldName =
                                ((LogSearchDTODecorator) dto).getOriginalFieldName(fieldName);
                    }

                    searchMethodToUse = fieldSearchMethodMap.get(configFieldName);
                    if (StringUtils.isBlank(searchMethodToUse)) {
                        throw new BusinessException(
                                ErrorCode.KEYWORD_FIELD_NOT_ALLOWED,
                                "字段 '" + configFieldName + "' 未配置默认搜索方法，请在请求中指定或在模块配置中设置");
                    }
                }

                // 获取对应的搜索方法枚举
                SearchMethod searchMethod = SearchMethod.fromString(searchMethodToUse);

                // 使用搜索方法枚举解析表达式
                String parsedCondition = searchMethod.parseExpression(fieldName, searchValue);

                if (StringUtils.isNotBlank(parsedCondition)) {
                    if (!isFirstCondition) {
                        condition.append(" AND ");
                    }
                    // 为每个字段的条件加括号
                    condition.append("(").append(parsedCondition).append(")");
                    isFirstCondition = false;
                }
            }
        }

        String result = condition.toString();

        // 只有多个字段条件时才需要外层括号，单个条件不需要额外的括号
        if (StringUtils.isNotBlank(result)) {
            // 计算实际有效条件的数量
            long validConditionCount =
                    keywordConditions.stream()
                            .filter(
                                    kc ->
                                            StringUtils.isNotBlank(kc.getFieldName())
                                                    && StringUtils.isNotBlank(kc.getSearchValue()))
                            .count();

            if (validConditionCount > 1) {
                return "(" + result + ")";
            } else {
                return result;
            }
        }

        return result;
    }
}
