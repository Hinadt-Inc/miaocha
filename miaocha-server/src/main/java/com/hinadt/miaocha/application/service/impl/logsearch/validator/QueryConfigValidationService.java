package com.hinadt.miaocha.application.service.impl.logsearch.validator;

import com.hinadt.miaocha.application.service.ModuleInfoService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.module.QueryConfigDTO;
import com.hinadt.miaocha.domain.dto.module.QueryConfigDTO.KeywordFieldConfigDTO;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 查询配置验证服务
 *
 * <p>负责： 1. 验证模块的查询配置是否完整 2. 获取字段对应的搜索方法 3. 获取配置的时间字段
 */
@Service
public class QueryConfigValidationService {

    @Autowired private ModuleInfoService moduleInfoService;

    /**
     * 验证模块配置并获取QueryConfigDTO
     *
     * @param module 模块名
     * @return QueryConfigDTO
     * @throws BusinessException 如果模块未配置查询信息
     */
    public QueryConfigDTO validateAndGetQueryConfig(String module) {
        QueryConfigDTO queryConfig = moduleInfoService.getQueryConfigByModule(module);
        if (queryConfig == null) {
            throw new BusinessException(
                    ErrorCode.MODULE_QUERY_CONFIG_NOT_FOUND, "模块 '" + module + "' 未配置查询信息");
        }
        return queryConfig;
    }

    /**
     * 获取配置的时间字段
     *
     * @param module 模块名
     * @return 时间字段名
     * @throws BusinessException 如果未配置时间字段
     */
    public String getTimeField(String module) {
        QueryConfigDTO queryConfig = validateAndGetQueryConfig(module);
        String timeField = queryConfig.getTimeField();
        if (timeField == null || timeField.trim().isEmpty()) {
            throw new BusinessException(
                    ErrorCode.TIME_FIELD_NOT_CONFIGURED, "模块 '" + module + "' 未配置时间字段");
        }
        return timeField;
    }

    /**
     * 获取字段的搜索方法
     *
     * @param module 模块名
     * @param fieldName 字段名
     * @return 搜索方法（LIKE, MATCH_ALL, MATCH_ANY, MATCH_PHRASE）
     * @throws BusinessException 如果字段未配置
     */
    public String getFieldSearchMethod(String module, String fieldName) {
        QueryConfigDTO queryConfig = validateAndGetQueryConfig(module);
        List<KeywordFieldConfigDTO> keywordFields = queryConfig.getKeywordFields();

        if (keywordFields == null) {
            throw new BusinessException(
                    ErrorCode.KEYWORD_FIELDS_NOT_CONFIGURED, "模块 '" + module + "' 未配置关键字查询字段");
        }

        return keywordFields.stream()
                .filter(field -> fieldName.equals(field.getFieldName()))
                .findFirst()
                .map(KeywordFieldConfigDTO::getSearchMethod)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        ErrorCode.KEYWORD_FIELD_NOT_ALLOWED,
                                        "字段 '" + fieldName + "' 不允许进行关键字查询"));
    }

    /**
     * 构建字段名到搜索方法的映射
     *
     * @param module 模块名
     * @return 字段名到搜索方法的映射
     */
    public Map<String, String> getFieldSearchMethodMap(String module) {
        QueryConfigDTO queryConfig = validateAndGetQueryConfig(module);
        List<KeywordFieldConfigDTO> keywordFields = queryConfig.getKeywordFields();

        if (keywordFields == null) {
            throw new BusinessException(
                    ErrorCode.KEYWORD_FIELDS_NOT_CONFIGURED, "模块 '" + module + "' 未配置关键字查询字段");
        }

        return keywordFields.stream()
                .collect(
                        Collectors.toMap(
                                KeywordFieldConfigDTO::getFieldName,
                                KeywordFieldConfigDTO::getSearchMethod));
    }
}
