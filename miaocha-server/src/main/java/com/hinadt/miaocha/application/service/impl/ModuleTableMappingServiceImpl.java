package com.hinadt.miaocha.application.service.impl;

import com.hinadt.miaocha.application.service.ModuleTableMappingService;
import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.mapper.ModuleInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 模块与表名映射服务实现类 */
@Service
public class ModuleTableMappingServiceImpl implements ModuleTableMappingService {

    @Autowired private ModuleInfoMapper moduleInfoMapper;

    @Override
    public String getTableNameByModule(String module) {
        if (!StringUtils.hasText(module)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块名称不能为空");
        }

        // 通过模块名称查询模块信息
        com.hinadt.miaocha.domain.entity.ModuleInfo moduleInfo =
                moduleInfoMapper.selectByName(module);
        if (moduleInfo == null) {
            throw new BusinessException(ErrorCode.MODULE_NOT_FOUND, "未找到模块: " + module);
        }

        String tableName = moduleInfo.getTableName();
        if (!StringUtils.hasText(tableName)) {
            throw new RuntimeException("模块 " + module + " 对应的表名未配置");
        }

        return tableName;
    }

    @Override
    public boolean moduleExists(String module) {
        if (!StringUtils.hasText(module)) {
            return false;
        }

        // 通过模块名称查询模块信息
        com.hinadt.miaocha.domain.entity.ModuleInfo moduleInfo =
                moduleInfoMapper.selectByName(module);
        return moduleInfo != null;
    }
}
