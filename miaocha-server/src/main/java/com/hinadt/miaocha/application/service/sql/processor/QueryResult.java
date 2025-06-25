package com.hinadt.miaocha.application.service.sql.processor;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 结构化查询结果
 *
 * <p>替代Map&lt;String, Object&gt;，提供类型安全的查询结果访问
 */
@Data
public class QueryResult {

    /** 列名列表 */
    private List<String> columns;

    /** 数据行列表，每行是一个Map，key为列名，value为列值 */
    private List<Map<String, Object>> rows;

    /** 总行数（用于分页） */
    private Integer totalCount;

    /** 是否有数据 */
    public boolean hasData() {
        return rows != null && !rows.isEmpty();
    }

    /** 获取第一行数据 */
    public Map<String, Object> getFirstRow() {
        return hasData() ? rows.get(0) : null;
    }

    /** 获取行数 */
    public int getRowCount() {
        return rows != null ? rows.size() : 0;
    }
}
