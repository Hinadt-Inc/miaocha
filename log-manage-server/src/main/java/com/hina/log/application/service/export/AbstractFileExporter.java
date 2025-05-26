package com.hina.log.application.service.export;

import com.hina.log.common.exception.BusinessException;
import com.hina.log.common.exception.ErrorCode;
import com.hina.log.domain.dto.SqlQueryResultDTO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/** 抽象文件导出器 实现一些通用的导出逻辑 */
public abstract class AbstractFileExporter implements FileExporter {

    private static final String EXPORT_ERROR_MSG = "导出失败: ";

    @Override
    public byte[] exportToBytes(SqlQueryResultDTO result) {
        validateResult(result);
        return doExportToBytes(result.getRows(), result.getColumns().toArray(new String[0]));
    }

    @Override
    public void exportToFile(SqlQueryResultDTO result, String filePath) throws IOException {
        validateResult(result);

        // 确保目录存在
        ensureDirectoryExists(filePath);

        // 调用具体导出方法
        exportToFile(result.getRows(), result.getColumns().toArray(new String[0]), filePath);
    }

    @Override
    public Resource exportToResource(SqlQueryResultDTO result) {
        validateResult(result);
        byte[] data = exportToBytes(result);
        return new ByteArrayResource(data);
    }

    /** 验证查询结果是否有效 */
    protected void validateResult(SqlQueryResultDTO result) {
        if (result == null
                || result.getRows() == null
                || result.getRows().isEmpty()
                || result.getColumns() == null
                || result.getColumns().isEmpty()) {
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "没有数据可导出");
        }
    }

    /** 确保目录存在 */
    protected void ensureDirectoryExists(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Path parentPath = path.getParent();
        if (parentPath != null && !Files.exists(parentPath)) {
            Files.createDirectories(parentPath);
        }
    }

    /** 具体实现类需要提供的二进制导出方法 */
    protected abstract byte[] doExportToBytes(List<Map<String, Object>> data, String[] headers);
}
