package com.hina.log.service.impl;

import com.hina.log.entity.LogstashProcess;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.LogstashProcessMapper;
import com.hina.log.service.ModuleTableMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 模块与表名映射服务实现类
 */
@Service
public class ModuleTableMappingServiceImpl implements ModuleTableMappingService {

    @Autowired
    private LogstashProcessMapper logstashProcessMapper;
    
    @Override
    public String getTableNameByModule(String module) {
        if (!StringUtils.hasText(module)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块名称不能为空");
        }
        
        // 使用精准SQL查询，只查询表名字段
        String tableName = logstashProcessMapper.selectTableNameByModule(module);
        if (tableName == null) {
            throw new BusinessException(ErrorCode.MODULE_NOT_FOUND, "未找到模块: " + module);
        }
        
        if (!StringUtils.hasText(tableName)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模块 " + module + " 对应的表名未配置");
        }
        
        return tableName;
    }
    
    @Override
    public boolean moduleExists(String module) {
        if (!StringUtils.hasText(module)) {
            return false;
        }
        
        // 使用精准SQL查询，只查询计数
        int count = logstashProcessMapper.countByModule(module);
        return count > 0;
    }
}
