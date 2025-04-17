package com.hina.log.service.database;

import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库元数据服务工厂
 * 根据数据库类型获取对应的元数据服务
 */
@Component
public class DatabaseMetadataServiceFactory {

    private final Map<String, DatabaseMetadataService> serviceMap = new HashMap<>();

    @Autowired
    public DatabaseMetadataServiceFactory(List<DatabaseMetadataService> services) {
        // 初始化服务映射
        for (DatabaseMetadataService service : services) {
            serviceMap.put(service.getSupportedDatabaseType().toLowerCase(), service);
        }
    }

    /**
     * 根据数据库类型获取对应的元数据服务
     * 
     * @param dbType 数据库类型
     * @return 对应的元数据服务
     * @throws BusinessException 如果不支持该数据库类型
     */
    public DatabaseMetadataService getService(String dbType) {
        if (dbType == null || dbType.isEmpty()) {
            throw new BusinessException(ErrorCode.DATASOURCE_TYPE_NOT_SUPPORTED, "未指定数据库类型");
        }

        // 兼容处理：doris当作mysql处理
        if ("doris".equalsIgnoreCase(dbType)) {
            dbType = "mysql";
        }

        DatabaseMetadataService service = serviceMap.get(dbType.toLowerCase());
        if (service == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_TYPE_NOT_SUPPORTED, "不支持的数据库类型: " + dbType);
        }

        return service;
    }
}