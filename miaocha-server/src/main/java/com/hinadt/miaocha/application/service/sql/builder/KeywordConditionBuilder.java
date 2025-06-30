package com.hinadt.miaocha.application.service.sql.builder;

import com.hinadt.miaocha.application.service.impl.logsearch.validator.QueryConfigValidationService;
import com.hinadt.miaocha.application.service.sql.search.SearchMethod;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.logsearch.KeywordConditionDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTODecorator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 关键字条件构建器
 *
 * <p>处理基于keywordConditions的查询条件构建，支持： - 多字段关键字查询（每个条件支持多个字段，字段间OR连接，条件间AND连接） - 配置驱动的搜索方法（LIKE,
 * MATCH_PHRASE, MATCH_ANY, MATCH_ALL） - 复杂表达式解析（&& 和 || 运算符，括号嵌套）
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
            if (keywordCondition.getFieldNames() != null
                    && !keywordCondition.getFieldNames().isEmpty()
                    && StringUtils.isNotBlank(keywordCondition.getSearchValue())) {

                String searchValue = keywordCondition.getSearchValue().trim();
                StringBuilder fieldConditions = new StringBuilder();
                boolean isFirstField = true;

                // 处理当前条件的每个字段
                for (String fieldName : keywordCondition.getFieldNames()) {
                    if (StringUtils.isNotBlank(fieldName)) {
                        String trimmedFieldName = fieldName.trim();

                        // 获取字段对应的搜索方法
                        String configFieldName = trimmedFieldName;
                        if (dto instanceof LogSearchDTODecorator) {
                            configFieldName =
                                    ((LogSearchDTODecorator) dto)
                                            .getOriginalFieldName(trimmedFieldName);
                        }

                        String searchMethodToUse = fieldSearchMethodMap.get(configFieldName);
                        if (StringUtils.isBlank(searchMethodToUse)) {
                            throw new BusinessException(
                                    ErrorCode.KEYWORD_FIELD_NOT_ALLOWED,
                                    "字段 '" + configFieldName + "' 未配置默认搜索方法，请在模块配置中设置");
                        }

                        // 获取对应的搜索方法枚举并解析表达式
                        SearchMethod searchMethod = SearchMethod.fromString(searchMethodToUse);
                        String parsedCondition =
                                searchMethod.parseExpression(trimmedFieldName, searchValue);

                        if (StringUtils.isNotBlank(parsedCondition)) {
                            if (!isFirstField) {
                                fieldConditions.append(" OR ");
                            }
                            // 每个字段条件都用括号包围
                            fieldConditions.append("(").append(parsedCondition).append(")");
                            isFirstField = false;
                        }
                    }
                }

                String currentCondition = fieldConditions.toString();
                if (StringUtils.isNotBlank(currentCondition)) {
                    if (!isFirstCondition) {
                        condition.append(" AND ");
                    }

                    // 如果是多字段，需要外层括号；单字段直接使用
                    if (keywordCondition.getFieldNames().size() > 1) {
                        condition.append("(").append(currentCondition).append(")");
                    } else {
                        condition.append(currentCondition);
                    }
                    isFirstCondition = false;
                }
            }
        }

        String result = condition.toString();

        // 只有多个关键字条件时才需要最外层括号
        if (StringUtils.isNotBlank(result)) {
            // 计算实际有效条件的数量
            long validConditionCount =
                    keywordConditions.stream()
                            .filter(
                                    kc ->
                                            kc.getFieldNames() != null
                                                    && !kc.getFieldNames().isEmpty()
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
