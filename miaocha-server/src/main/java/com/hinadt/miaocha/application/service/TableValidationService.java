package com.hinadt.miaocha.application.service;

import com.hinadt.miaocha.domain.entity.ModuleInfo;

/** 表验证服务接口 用于验证SQL语句和表相关操作 */
public interface TableValidationService {

    /**
     * 验证Doris SQL语句 校验规则： 1. 只能执行CREATE TABLE语句 2. 表名必须与模块配置的表名一致
     *
     * @param moduleInfo 模块信息
     * @param sql SQL语句
     * @throws BusinessException 如果SQL不符合要求
     */
    void validateDorisSql(ModuleInfo moduleInfo, String sql);
}
