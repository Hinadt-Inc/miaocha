package com.hina.log.service.impl;

import com.hina.log.entity.LogstashProcess;
import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import com.hina.log.mapper.LogstashProcessMapper;
import com.hina.log.service.ModuleTableMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块与表名映射服务实现类
 */
@Service
public class ModuleTableMappingServiceImpl implements ModuleTableMappingService {

    @Autowired
    private LogstashProcessMapper logstashProcessMapper;
    
    // 缓存模块与表名的映射关系，提高性能
    private final Map<String, String> moduleTableCache = new ConcurrentHashMap<>();
    
    @Override
    public String getTableNameByModule(String module) {
        if (!StringUtils.hasText(module)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "模块名称不能为空");
        }
        
        // 先从缓存中查找
        String tableName = moduleTableCache.get(module);
        if (tableName != null) {
            return tableName;
        }
        
        // 从数据库中查找
        List<LogstashProcess> processes = logstashProcessMapper.selectAll();
        for (LogstashProcess process : processes) {
            if (module.equals(process.getModule())) {
                if (!StringUtils.hasText(process.getTableName())) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模块 " + module + " 对应的表名未配置");
                }
                
                // 更新缓存
                moduleTableCache.put(module, process.getTableName());
                return process.getTableName();
            }
        }
        
        throw new BusinessException(ErrorCode.MODULE_NOT_FOUND, "未找到模块: " + module);
    }
    
    @Override
    public boolean moduleExists(String module) {
        if (!StringUtils.hasText(module)) {
            return false;
        }
        
        // 先从缓存中查找
        if (moduleTableCache.containsKey(module)) {
            return true;
        }
        
        // 从数据库中查找
        List<LogstashProcess> processes = logstashProcessMapper.selectAll();
        for (LogstashProcess process : processes) {
            if (module.equals(process.getModule())) {
                // 如果找到了模块，但表名为空，也认为模块存在
                if (StringUtils.hasText(process.getTableName())) {
                    moduleTableCache.put(module, process.getTableName());
                }
                return true;
            }
        }
        
        return false;
    }
}
