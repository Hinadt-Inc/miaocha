package com.hina.log.service.database;

import com.hina.log.dto.SchemaInfoDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据库元数据服务接口
 * 定义获取数据库元数据的方法
 */
public interface DatabaseMetadataService {

    /**
     * 获取所有表名
     * 
     * @param connection 数据库连接
     * @return 表名列表
     * @throws SQLException 如果数据库操作失败
     */
    List<String> getAllTables(Connection connection) throws SQLException;

    /**
     * 获取表注释
     * 
     * @param connection 数据库连接
     * @param tableName  表名
     * @return 表注释，如果没有则返回空字符串
     */
    String getTableComment(Connection connection, String tableName);

    /**
     * 获取表字段信息
     * 
     * @param connection 数据库连接
     * @param tableName  表名
     * @return 字段信息列表
     * @throws SQLException 如果数据库操作失败
     */
    List<SchemaInfoDTO.ColumnInfoDTO> getColumnInfo(Connection connection, String tableName) throws SQLException;

    /**
     * 此服务支持的数据库类型
     * 
     * @return 数据库类型名称
     */
    String getSupportedDatabaseType();
}