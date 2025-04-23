package com.hina.log.service.export;

import com.hina.log.exception.BusinessException;
import com.hina.log.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件导出器工厂
 * 根据文件扩展名获取对应的导出器
 */
@Component
public class FileExporterFactory {

    private final Map<String, FileExporter> exporterMap = new HashMap<>();

    @Autowired
    public FileExporterFactory(List<FileExporter> exporters) {
        // 初始化导出器映射
        for (FileExporter exporter : exporters) {
            exporterMap.put(exporter.getSupportedExtension().toLowerCase(), exporter);
        }
    }

    /**
     * 根据文件类型获取对应的导出器
     * 
     * @param fileType 文件类型扩展名，不含点，如"csv"、"xlsx"
     * @return 对应的文件导出器
     * @throws BusinessException 如果不支持该文件类型
     */
    public FileExporter getExporter(String fileType) {
        if (fileType == null || fileType.isEmpty()) {
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "未指定文件类型");
        }

        FileExporter exporter = exporterMap.get(fileType.toLowerCase());
        if (exporter == null) {
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "不支持的文件类型: " + fileType);
        }

        return exporter;
    }

    /**
     * 从文件路径中提取文件类型并获取对应的导出器
     * 
     * @param filePath 文件路径
     * @return 对应的文件导出器
     * @throws BusinessException 如果不支持该文件类型
     */
    public FileExporter getExporterFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "文件路径为空");
        }

        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == filePath.length() - 1) {
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "无法从文件路径中获取文件类型: " + filePath);
        }

        String fileType = filePath.substring(lastDotIndex + 1);
        return getExporter(fileType);
    }
}