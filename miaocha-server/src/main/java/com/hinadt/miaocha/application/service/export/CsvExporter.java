package com.hinadt.miaocha.application.service.export;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** CSV文件导出器 */
@Component
public class CsvExporter extends AbstractFileExporter {

    private static final char CSV_DELIMITER = ',';
    private static final char CSV_QUOTE = '"';
    private static final String CSV_LINE_END = "\n";

    @Override
    public void exportToFile(List<Map<String, Object>> data, String[] headers, String filePath)
            throws IOException {
        // 确保目录存在
        ensureDirectoryExists(filePath);

        try (Writer writer =
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            // 写入表头
            writer.write(formatCsvRow(headers));
            writer.write(CSV_LINE_END);

            // 写入数据行
            for (Map<String, Object> row : data) {
                List<String> rowValues = new ArrayList<>();
                for (String header : headers) {
                    Object value = row.get(header);
                    rowValues.add(value != null ? value.toString() : "");
                }
                writer.write(formatCsvRow(rowValues.toArray(new String[0])));
                writer.write(CSV_LINE_END);
            }
        }
    }

    @Override
    protected byte[] doExportToBytes(List<Map<String, Object>> data, String[] headers) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {

            // 写入表头
            writer.write(formatCsvRow(headers));
            writer.write(CSV_LINE_END);

            // 写入数据行
            for (Map<String, Object> row : data) {
                List<String> rowValues = new ArrayList<>();
                for (String header : headers) {
                    Object value = row.get(header);
                    rowValues.add(value != null ? value.toString() : "");
                }
                writer.write(formatCsvRow(rowValues.toArray(new String[0])));
                writer.write(CSV_LINE_END);
            }

            writer.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出CSV失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getSupportedExtension() {
        return "csv";
    }

    /** 格式化CSV行，处理值中包含逗号、引号等特殊字符的情况 */
    private String formatCsvRow(String[] values) {
        if (values == null || values.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(CSV_DELIMITER);
            }

            String value = values[i];
            if (value == null) {
                value = "";
            }

            // 如果值包含逗号、引号或换行符，需要用引号括起来
            if (value.contains(String.valueOf(CSV_DELIMITER))
                    || value.contains(String.valueOf(CSV_QUOTE))
                    || value.contains("\n")
                    || value.contains("\r")) {

                // 将值中的引号替换为双引号
                value =
                        value.replace(
                                String.valueOf(CSV_QUOTE), String.valueOf(CSV_QUOTE) + CSV_QUOTE);

                // 用引号括起来
                sb.append(CSV_QUOTE).append(value).append(CSV_QUOTE);
            } else {
                sb.append(value);
            }
        }

        return sb.toString();
    }
}
