package com.hinadt.miaocha.application.service.impl.logsearch.validator;

import com.hinadt.miaocha.application.service.DatasourceService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 日志搜索验证工具类
 *
 * <p>集中处理所有验证逻辑
 */
@Component
public class LogSearchValidator {

    /** 分页查询的最大页面大小限制 */
    private static final int MAX_PAGE_SIZE = 5000;

    private final DatasourceService datasourceService;

    public LogSearchValidator(DatasourceService datasourceService) {
        this.datasourceService = datasourceService;
    }

    /** 验证并获取数据源（通过模块名称） */
    public DatasourceInfo validateAndGetDatasource(String module) {
        return datasourceService.getDatasourceInfoByModule(module);
    }

    /** 验证分页参数 */
    public void validatePaginationParams(LogSearchDTO dto) {
        if (dto.getPageSize() != null && dto.getPageSize() > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "页面大小不能超过 " + MAX_PAGE_SIZE);
        }
    }

    /** 验证字段列表 */
    public void validateFields(LogSearchDTO dto) {
        if (dto.getFields() == null || dto.getFields().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "字段列表不能为空");
        }
    }

    /** 验证排序字段列表 */
    public void validateSortFields(LogSearchDTO dto) {
        if (dto.getSortFields() == null || dto.getSortFields().isEmpty()) {
            return; // 排序字段是可选的
        }

        // 验证字段名不重复
        Set<String> fieldNames = new HashSet<>();
        for (LogSearchDTO.SortField sortField : dto.getSortFields()) {
            String fieldName = sortField.getFieldName();
            if (fieldName != null && !fieldNames.add(fieldName)) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR, "排序字段不能重复，重复字段: " + fieldName);
            }
        }
    }
}
