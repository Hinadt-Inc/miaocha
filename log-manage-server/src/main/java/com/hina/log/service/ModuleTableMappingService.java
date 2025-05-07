package com.hina.log.service;

/**
 * 模块与表名映射服务接口
 * 用于根据模块名称获取对应的表名
 */
public interface ModuleTableMappingService {
    
    /**
     * 根据模块名称获取对应的表名
     *
     * @param module 模块名称
     * @return 表名
     */
    String getTableNameByModule(String module);
    
    /**
     * 检查模块是否存在
     *
     * @param module 模块名称
     * @return 是否存在
     */
    boolean moduleExists(String module);
}
