package com.hinadt.miaocha.application.service.sql;

import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** Light-weight helpers for SELECT detection, LIMIT handling, and table name extraction. */
public final class SqlQueryUtils {
    private static final Pattern LIMIT_PATTERN =
            Pattern.compile(
                    "\\blimit\\s+\\d+(?:\\s*,\\s*\\d+)?\\s*;?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FROM_PATTERN =
            Pattern.compile("\\bFROM\\s+[\"'`]?([\\w\\d_\\.]+)[\"'`]?", Pattern.CASE_INSENSITIVE);

    private static final int DEFAULT_QUERY_LIMIT = 1000;
    private static final int MAX_QUERY_LIMIT = 10000;

    private SqlQueryUtils() {}

    public static boolean isSelectStatement(String sql) {
        return isSelectStatementInternal(removeCommentsAndWhitespace(sql));
    }

    public static String processSqlWithLimit(String sql) {
        if (sql == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "SQL语句不能为空");
        if (!isSelectStatement(sql)) return sql;
        String sqlLower = sql.trim().toLowerCase();
        java.util.regex.Matcher limitMatcher = LIMIT_PATTERN.matcher(sqlLower);
        if (limitMatcher.find()) {
            int limitValue = extractLimitValue(limitMatcher.group(0));
            if (limitValue > MAX_QUERY_LIMIT) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "查询结果数量限制不能超过" + MAX_QUERY_LIMIT + "条，请调整您的LIMIT语句");
            }
            return sql;
        }
        if (sqlLower.endsWith(";"))
            return sql.substring(0, sql.length() - 1) + " LIMIT " + DEFAULT_QUERY_LIMIT + ";";
        return sql + " LIMIT " + DEFAULT_QUERY_LIMIT;
    }

    public static Set<String> extractTableNames(String sql) {
        Set<String> tableNames = new HashSet<>();
        if (sql == null || sql.trim().isEmpty()) return tableNames;
        java.util.regex.Matcher fromMatcher = FROM_PATTERN.matcher(sql);
        while (fromMatcher.find()) {
            String tableName = fromMatcher.group(1);
            if (tableName != null && !tableName.trim().isEmpty()) {
                if (tableName.contains(".")) {
                    String[] parts = tableName.split("\\.");
                    tableName = parts[parts.length - 1];
                }
                tableNames.add(tableName.trim());
            }
        }
        return tableNames;
    }

    private static int extractLimitValue(String limitClause) {
        String numbers = limitClause.replaceAll("\\blimit\\s+", "").trim();
        if (numbers.endsWith(";")) numbers = numbers.substring(0, numbers.length() - 1).trim();
        if (numbers.contains(",")) {
            String[] parts = numbers.split(",");
            return Integer.parseInt(parts[1].trim());
        }
        return Integer.parseInt(numbers.trim());
    }

    private static String removeCommentsAndWhitespace(String sql) {
        if (sql == null || sql.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        char[] chars = sql.toCharArray();
        int length = chars.length;
        boolean inSingleQuotes = false, inDoubleQuotes = false, inBackticks = false;
        boolean inLineComment = false, inBlockComment = false;
        int blockCommentDepth = 0;
        for (int i = 0; i < length; i++) {
            char current = chars[i];
            char next = (i + 1 < length) ? chars[i + 1] : '\0';
            char prev = (i > 0) ? chars[i - 1] : '\0';
            if (prev == '\\' && (inSingleQuotes || inDoubleQuotes)) {
                if (!inLineComment && !inBlockComment) {
                    result.append(current);
                }
                continue;
            }
            if (!inLineComment && !inBlockComment) {
                if (current == '\'' && !inDoubleQuotes && !inBackticks) {
                    inSingleQuotes = !inSingleQuotes;
                    result.append(current);
                    continue;
                } else if (current == '"' && !inSingleQuotes && !inBackticks) {
                    inDoubleQuotes = !inDoubleQuotes;
                    result.append(current);
                    continue;
                } else if (current == '`' && !inSingleQuotes && !inDoubleQuotes) {
                    inBackticks = !inBackticks;
                    result.append(current);
                    continue;
                }
            }
            if (inSingleQuotes || inDoubleQuotes || inBackticks) {
                if (!inLineComment && !inBlockComment) {
                    result.append(current);
                }
                continue;
            }
            if (!inLineComment && !inBlockComment) {
                if (current == '-' && next == '-') {
                    inLineComment = true;
                    i++;
                    continue;
                }
                if (current == '/' && next == '*') {
                    inBlockComment = true;
                    blockCommentDepth = 1;
                    i++;
                    continue;
                }
            }
            if (inBlockComment) {
                if (current == '/' && next == '*') {
                    blockCommentDepth++;
                    i++;
                    continue;
                } else if (current == '*' && next == '/') {
                    blockCommentDepth--;
                    if (blockCommentDepth == 0) inBlockComment = false;
                    i++;
                    continue;
                }
                continue;
            }
            if (inLineComment) {
                if (current == '\n' || current == '\r') {
                    inLineComment = false;
                    result.append(' ');
                }
                continue;
            }
            if (Character.isWhitespace(current)) {
                if (result.length() > 0 && result.charAt(result.length() - 1) != ' ') {
                    result.append(' ');
                }
            } else {
                result.append(current);
            }
        }
        return result.toString().trim();
    }

    private static boolean isSelectStatementInternal(String cleanedSql) {
        if (cleanedSql == null || cleanedSql.isEmpty()) return false;
        String upperSql = cleanedSql.toUpperCase().trim();
        while (upperSql.startsWith("(")) {
            upperSql = upperSql.substring(1).trim();
        }
        return upperSql.startsWith("SELECT ")
                || upperSql.startsWith("WITH ")
                || upperSql.startsWith("(SELECT ")
                || upperSql.matches("^\\s*\\(\\s*WITH\\s+.*");
    }
}
