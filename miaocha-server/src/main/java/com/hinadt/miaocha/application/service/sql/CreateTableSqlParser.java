package com.hinadt.miaocha.application.service.sql;

import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for parsing CREATE TABLE DDL: extract table name and field names. Pure functions with no
 * logging/state, suitable for reuse and testing.
 */
public final class CreateTableSqlParser {
    private CreateTableSqlParser() {}

    public static boolean isCreateTable(String sql) {
        if (sql == null) return false;
        String s = sql.trim().toUpperCase();
        return s.startsWith("CREATE TABLE ")
                || s.startsWith("CREATE\nTABLE ")
                || s.startsWith("CREATE\r\nTABLE ");
    }

    public static String extractTableName(String sql) {
        if (sql == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SQL语句不能为空");
        }
        String normalized = sql.replaceAll("\\s+", " ").trim();
        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile(
                        "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(.+?)\\s*\\(",
                        java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(normalized);
        if (!matcher.find()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "无法从SQL语句中解析出表名");
        }
        String tableNamePart = matcher.group(1).trim();
        if (tableNamePart.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "表名不能为空");
        }
        if (tableNamePart.toUpperCase().matches(".*\\b(IF|NOT|EXISTS|EXIST)\\b.*")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "表名格式错误或SQL语法错误");
        }
        return parseTableName(tableNamePart);
    }

    private static String parseTableName(String tableNamePart) {
        if (tableNamePart.contains(".")) {
            return parseTableNameWithDatabase(tableNamePart);
        }
        if (tableNamePart.startsWith("`")
                && tableNamePart.endsWith("`")
                && tableNamePart.length() > 2) {
            return tableNamePart.substring(1, tableNamePart.length() - 1);
        }
        return tableNamePart;
    }

    private static String parseTableNameWithDatabase(String tableNamePart) {
        String input = tableNamePart.trim();
        int lastDotIndex = -1;
        boolean inBackticks = false;
        for (int i = input.length() - 1; i >= 0; i--) {
            char c = input.charAt(i);
            if (c == '`') {
                inBackticks = !inBackticks;
            } else if (c == '.' && !inBackticks) {
                lastDotIndex = i;
                break;
            }
        }
        if (lastDotIndex == -1) {
            return input.replace("`", "").trim();
        }
        if (lastDotIndex == input.length() - 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "表名格式错误：表名不能为空");
        }
        if (lastDotIndex == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "表名格式错误：数据库名不能为空");
        }
        String tablePart = input.substring(lastDotIndex + 1).trim();
        if (tablePart.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "表名不能为空");
        }
        return tablePart.replace("`", "").trim();
    }

    public static List<String> extractFieldNames(String sql) {
        List<String> fieldNames = new ArrayList<>();
        if (sql == null || sql.trim().isEmpty()) return fieldNames;
        String section = extractFieldsSection(sql);
        if (section == null) return fieldNames;
        String noComments = removeSqlCommentsPreserveLines(section);
        String filtered = removeNonFieldDefinitions(noComments);
        List<String> defs = smartSplitFieldDefinitions(filtered);
        for (String def : defs) {
            String name = extractFieldName(def.trim());
            if (name != null && !name.isEmpty()) fieldNames.add(name);
        }
        return fieldNames;
    }

    private static String extractFieldsSection(String sql) {
        char[] chars = sql.toCharArray();
        int n = chars.length;
        boolean inSingleQuotes = false, inDoubleQuotes = false, inBackticks = false;
        boolean inLineComment = false, inBlockComment = false;
        int blockDepth = 0, depth = 0, startIndex = -1;
        for (int i = 0; i < n; i++) {
            char c = chars[i];
            char next = (i + 1 < n) ? chars[i + 1] : '\0';
            char prev = (i > 0) ? chars[i - 1] : '\0';
            if (!inSingleQuotes && !inDoubleQuotes && !inBackticks) {
                if (!inLineComment && !inBlockComment) {
                    if (c == '-' && next == '-') {
                        inLineComment = true;
                        i++;
                        continue;
                    }
                    if (c == '/' && next == '*') {
                        inBlockComment = true;
                        blockDepth = 1;
                        i++;
                        continue;
                    }
                } else if (inBlockComment) {
                    if (c == '/' && next == '*') {
                        blockDepth++;
                        i++;
                        continue;
                    }
                    if (c == '*' && next == '/') {
                        blockDepth--;
                        if (blockDepth == 0) inBlockComment = false;
                        i++;
                        continue;
                    }
                    continue;
                }
                if (inLineComment) {
                    if (c == '\n' || c == '\r') inLineComment = false;
                    continue;
                }
            }
            if (!inLineComment && !inBlockComment) {
                if (c == '\'' && !inDoubleQuotes && !inBackticks && prev != '\\')
                    inSingleQuotes = !inSingleQuotes;
                else if (c == '"' && !inSingleQuotes && !inBackticks && prev != '\\')
                    inDoubleQuotes = !inDoubleQuotes;
                else if (c == '`' && !inSingleQuotes && !inDoubleQuotes) inBackticks = !inBackticks;
            }
            if (inSingleQuotes || inDoubleQuotes || inBackticks || inLineComment || inBlockComment)
                continue;
            if (c == '(') {
                if (startIndex == -1) startIndex = i + 1;
                depth++;
            } else if (c == ')') {
                if (depth > 0 && --depth == 0 && startIndex != -1)
                    return sql.substring(startIndex, i);
            }
        }
        return null;
    }

    private static String removeSqlCommentsPreserveLines(String sql) {
        StringBuilder out = new StringBuilder();
        char[] chars = sql.toCharArray();
        int n = chars.length;
        boolean inSingle = false, inDouble = false, inBacktick = false;
        boolean inLine = false, inBlock = false;
        int blockDepth = 0;
        for (int i = 0; i < n; i++) {
            char c = chars[i];
            char next = (i + 1 < n) ? chars[i + 1] : '\0';
            char prev = (i > 0) ? chars[i - 1] : '\0';
            if (!inSingle && !inDouble && !inBacktick) {
                if (!inLine && !inBlock) {
                    if (c == '-' && next == '-') {
                        inLine = true;
                        i++;
                        continue;
                    }
                    if (c == '/' && next == '*') {
                        inBlock = true;
                        blockDepth = 1;
                        i++;
                        continue;
                    }
                } else if (inBlock) {
                    if (c == '/' && next == '*') {
                        blockDepth++;
                        i++;
                        continue;
                    }
                    if (c == '*' && next == '/') {
                        blockDepth--;
                        if (blockDepth == 0) inBlock = false;
                        i++;
                        continue;
                    }
                    continue;
                }
                if (inLine) {
                    if (c == '\n' || c == '\r') {
                        inLine = false;
                        out.append(c);
                    }
                    continue;
                }
            }
            if (!inLine && !inBlock) {
                if (c == '\'' && !inDouble && !inBacktick && prev != '\\') inSingle = !inSingle;
                else if (c == '"' && !inSingle && !inBacktick && prev != '\\') inDouble = !inDouble;
                else if (c == '`' && !inSingle && !inDouble) inBacktick = !inBacktick;
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String removeNonFieldDefinitions(String fieldsSection) {
        String[] lines = fieldsSection.split("\\n");
        StringBuilder cleaned = new StringBuilder();
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()
                    || trimmedLine.matches(
                            "(?i)^(PRIMARY\\s+KEY|UNIQUE\\s+KEY|KEY|INDEX|FOREIGN\\s+KEY|CONSTRAINT)\\b.*")
                    || trimmedLine.matches(
                            "(?i)^\\)\\s*(ENGINE|DUPLICATE|AUTO|DISTRIBUTED|PROPERTIES)\\b.*")) {
                continue;
            }
            cleaned.append(line).append("\n");
        }
        return cleaned.toString();
    }

    private static List<String> smartSplitFieldDefinitions(String fieldsSection) {
        List<String> definitions = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inQuotes = false, inBackticks = false;
        char last = ' ';
        for (char c : fieldsSection.toCharArray()) {
            if (c == '`') {
                if (last != '\\') inBackticks = !inBackticks;
            } else if (c == '\'' || c == '"') {
                if (last != '\\') inQuotes = !inQuotes;
            } else if (!inQuotes && !inBackticks) {
                if (c == '(') depth++;
                else if (c == ')') depth--;
                else if (c == ',' && depth == 0) {
                    String def = current.toString().trim();
                    if (!def.isEmpty()) definitions.add(def);
                    current = new StringBuilder();
                    last = c;
                    continue;
                }
            }
            current.append(c);
            last = c;
        }
        String def = current.toString().trim();
        if (!def.isEmpty()) definitions.add(def);
        return definitions;
    }

    private static String extractFieldName(String fieldDefinition) {
        if (fieldDefinition == null || fieldDefinition.trim().isEmpty()) return null;
        String trimmed = fieldDefinition.trim();
        if (trimmed.matches(
                "(?i)^(PRIMARY\\s+KEY|UNIQUE\\s+KEY|KEY|INDEX|FOREIGN\\s+KEY|CONSTRAINT)\\b.*"))
            return null;
        String[] parts = trimmed.split("\\s+", 2);
        if (parts.length == 0) return null;
        String fieldName = parts[0];
        if (fieldName.startsWith("`") && fieldName.endsWith("`") && fieldName.length() > 2) {
            fieldName = fieldName.substring(1, fieldName.length() - 1);
        }
        if (fieldName.matches("^[a-zA-Z0-9_]+$")) return fieldName;
        return null;
    }
}
