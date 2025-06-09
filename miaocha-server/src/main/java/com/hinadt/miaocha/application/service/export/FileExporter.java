package com.hinadt.miaocha.application.service.export;

import com.hinadt.miaocha.domain.dto.SqlQueryResultDTO;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;

/** 文件导出接口 定义所有导出器的通用方法 */
public interface FileExporter {

    /**
     * 将查询结果导出为二进制数据
     *
     * @param result SQL查询结果
     * @return 二进制数据
     */
    byte[] exportToBytes(SqlQueryResultDTO result);

    /**
     * 将数据导出为文件
     *
     * @param data 数据行列表
     * @param headers 列名数组
     * @param filePath 文件路径
     * @throws IOException 如果导出失败
     */
    void exportToFile(List<Map<String, Object>> data, String[] headers, String filePath)
            throws IOException;

    /**
     * 将查询结果导出为文件
     *
     * @param result SQL查询结果
     * @param filePath 文件路径
     * @throws IOException 如果导出失败
     */
    void exportToFile(SqlQueryResultDTO result, String filePath) throws IOException;

    /**
     * 将查询结果转换为资源对象
     *
     * @param result SQL查询结果
     * @return 资源对象
     */
    Resource exportToResource(SqlQueryResultDTO result);

    /**
     * 获取此导出器支持的文件扩展名
     *
     * @return 文件扩展名（不包含点）
     */
    String getSupportedExtension();
}
