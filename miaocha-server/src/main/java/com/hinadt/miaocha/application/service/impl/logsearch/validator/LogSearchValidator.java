package com.hinadt.miaocha.application.service.impl.logsearch.validator;

import com.hinadt.miaocha.application.service.DatasourceService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.LogSearchDTO;
import com.hinadt.miaocha.domain.entity.DatasourceInfo;
import com.hinadt.miaocha.domain.entity.User;
import com.hinadt.miaocha.domain.mapper.UserMapper;
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

    private final UserMapper userMapper;
    private final DatasourceService datasourceService;

    public LogSearchValidator(UserMapper userMapper, DatasourceService datasourceService) {
        this.userMapper = userMapper;
        this.datasourceService = datasourceService;
    }

    /** 验证并获取数据源（通过模块名称） */
    public DatasourceInfo validateAndGetDatasource(String module) {
        return datasourceService.getDatasourceInfoByModule(module);
    }

    /** 验证用户 */
    public void validateUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
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
}
