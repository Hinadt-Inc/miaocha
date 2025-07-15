package com.hinadt.miaocha.domain.converter;

import com.hinadt.miaocha.domain.dto.DatabaseTableListDTO;
import com.hinadt.miaocha.domain.dto.SchemaInfoDTO;
import com.hinadt.miaocha.domain.dto.TableSchemaDTO;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Schema相关DTO转换器 */
@Component
public class SchemaConverter {

    /**
     * 将完整的 SchemaInfoDTO 转换为 DatabaseTableListDTO 提取表的基本信息，不包含字段信息
     *
     * @param schemaInfo 完整的schema信息
     * @return 数据库表列表
     */
    public DatabaseTableListDTO toTableListDTO(SchemaInfoDTO schemaInfo) {
        if (schemaInfo == null) {
            return null;
        }

        DatabaseTableListDTO tableListDTO = new DatabaseTableListDTO();
        tableListDTO.setDatabaseName(schemaInfo.getDatabaseName());

        if (schemaInfo.getTables() != null) {
            List<DatabaseTableListDTO.TableBasicInfoDTO> tables =
                    schemaInfo.getTables().stream()
                            .map(this::toTableBasicInfoDTO)
                            .collect(Collectors.toList());
            tableListDTO.setTables(tables);
        }

        return tableListDTO;
    }

    /** 将 SchemaInfoDTO.TableInfoDTO 转换为 TableBasicInfoDTO */
    private DatabaseTableListDTO.TableBasicInfoDTO toTableBasicInfoDTO(
            SchemaInfoDTO.TableInfoDTO tableInfo) {
        if (tableInfo == null) {
            return null;
        }

        DatabaseTableListDTO.TableBasicInfoDTO basicInfo =
                new DatabaseTableListDTO.TableBasicInfoDTO();
        basicInfo.setTableName(tableInfo.getTableName());
        basicInfo.setTableComment(tableInfo.getTableComment());
        return basicInfo;
    }

    /**
     * 将 SchemaInfoDTO.TableInfoDTO 转换为 TableSchemaDTO 用于从完整的表信息中提取单个表的schema信息
     *
     * @param tableInfo 表信息
     * @param databaseName 数据库名称
     * @return 表schema信息
     */
    public TableSchemaDTO toTableSchemaDTO(
            SchemaInfoDTO.TableInfoDTO tableInfo, String databaseName) {
        if (tableInfo == null) {
            return null;
        }

        TableSchemaDTO tableSchemaDTO = new TableSchemaDTO();
        tableSchemaDTO.setDatabaseName(databaseName);
        tableSchemaDTO.setTableName(tableInfo.getTableName());
        tableSchemaDTO.setTableComment(tableInfo.getTableComment());

        if (tableInfo.getColumns() != null) {
            List<TableSchemaDTO.ColumnInfoDTO> columns =
                    tableInfo.getColumns().stream()
                            .map(this::toTableSchemaColumnInfoDTO)
                            .collect(Collectors.toList());
            tableSchemaDTO.setColumns(columns);
        }

        return tableSchemaDTO;
    }

    /** 将 SchemaInfoDTO.ColumnInfoDTO 转换为 TableSchemaDTO.ColumnInfoDTO */
    private TableSchemaDTO.ColumnInfoDTO toTableSchemaColumnInfoDTO(
            SchemaInfoDTO.ColumnInfoDTO original) {
        if (original == null) {
            return null;
        }

        TableSchemaDTO.ColumnInfoDTO column = new TableSchemaDTO.ColumnInfoDTO();
        column.setColumnName(original.getColumnName());
        column.setDataType(original.getDataType());
        column.setIsPrimaryKey(original.getIsPrimaryKey());
        column.setIsNullable(original.getIsNullable());
        return column;
    }

    /**
     * 创建表基本信息列表 用于从权限表列表和元数据服务结果创建表列表
     *
     * @param tableNames 表名列表
     * @param tableComments 表注释映射（表名 -> 注释）
     * @return 表基本信息列表
     */
    public List<DatabaseTableListDTO.TableBasicInfoDTO> createTableBasicInfoList(
            List<String> tableNames, java.util.Map<String, String> tableComments) {
        if (tableNames == null) {
            return null;
        }

        return tableNames.stream()
                .map(
                        tableName -> {
                            DatabaseTableListDTO.TableBasicInfoDTO tableInfo =
                                    new DatabaseTableListDTO.TableBasicInfoDTO();
                            tableInfo.setTableName(tableName);
                            tableInfo.setTableComment(
                                    tableComments != null ? tableComments.get(tableName) : null);
                            return tableInfo;
                        })
                .collect(Collectors.toList());
    }

    /**
     * 创建表schema信息 用于从元数据服务结果创建表schema
     *
     * @param databaseName 数据库名称
     * @param tableName 表名
     * @param tableComment 表注释
     * @param columns 字段列表
     * @return 表schema信息
     */
    public TableSchemaDTO createTableSchema(
            String databaseName,
            String tableName,
            String tableComment,
            List<SchemaInfoDTO.ColumnInfoDTO> columns) {
        TableSchemaDTO tableSchemaDTO = new TableSchemaDTO();
        tableSchemaDTO.setDatabaseName(databaseName);
        tableSchemaDTO.setTableName(tableName);
        tableSchemaDTO.setTableComment(tableComment);

        if (columns != null) {
            List<TableSchemaDTO.ColumnInfoDTO> convertedColumns =
                    columns.stream()
                            .map(this::toTableSchemaColumnInfoDTO)
                            .collect(Collectors.toList());
            tableSchemaDTO.setColumns(convertedColumns);
        }

        return tableSchemaDTO;
    }
}
