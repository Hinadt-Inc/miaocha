package com.hina.log.application.service.export;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Excel文件导出器 */
@Component
public class ExcelExporter extends AbstractFileExporter {

    private static final String SHEET_NAME = "Data";
    private static final int DEFAULT_COLUMN_WIDTH = 5000;

    @Override
    public void exportToFile(List<Map<String, Object>> data, String[] headers, String filePath)
            throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            createWorkbookContent(workbook, data, headers);

            // 写入文件
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                workbook.write(outputStream);
            }
        }
    }

    @Override
    protected byte[] doExportToBytes(List<Map<String, Object>> data, String[] headers) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            createWorkbookContent(workbook, data, headers);

            // 写入字节流
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getSupportedExtension() {
        return "xlsx";
    }

    /** 创建工作簿内容 */
    private void createWorkbookContent(
            Workbook workbook, List<Map<String, Object>> data, String[] headers) {
        Sheet sheet = workbook.createSheet(SHEET_NAME);

        // 创建表头样式
        CellStyle headerStyle = createHeaderStyle(workbook);

        // 写入表头
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, DEFAULT_COLUMN_WIDTH);
        }

        // 写入数据行
        int rowNum = 1;
        for (Map<String, Object> rowData : data) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;

            for (String header : headers) {
                Cell cell = row.createCell(colNum++);
                setCellValue(cell, rowData.get(header));
            }
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /** 设置单元格值，根据数据类型自动匹配 */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof java.util.Date) {
            cell.setCellValue((java.util.Date) value);
            // 设置日期格式
            Workbook workbook = cell.getSheet().getWorkbook();
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
            cell.setCellStyle(dateStyle);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /** 创建表头样式 */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 设置字体
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        // 设置边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);

        // 设置对齐方式
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }
}
