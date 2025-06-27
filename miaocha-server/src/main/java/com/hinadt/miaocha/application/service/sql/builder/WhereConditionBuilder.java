package com.hinadt.miaocha.application.service.sql.builder;

import com.hinadt.miaocha.common.exception.BusinessException;
import com.hinadt.miaocha.common.exception.ErrorCode;
import com.hinadt.miaocha.domain.dto.logsearch.LogSearchDTO;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * WHERE条件构建器
 *
 * <p>专门负责处理用户提供的自定义WHERE条件（whereSqls）
 *
 * <p>功能特性：
 *
 * <ul>
 *   <li>拼接多个WHERE条件，使用AND连接
 *   <li>过滤空白条件
 *   <li>为多条件添加外层括号
 *   <li>配合DTO转换器，支持Variant字段的点语法转换
 *   <li>基础SQL注入防护
 * </ul>
 */
@Component
public class WhereConditionBuilder {

    /** 危险SQL关键字模式 - 防止基础SQL注入攻击 */
    private static final Pattern DANGEROUS_SQL_PATTERN =
            Pattern.compile(
                    "(?i)\\b(DROP|DELETE|INSERT|UPDATE|CREATE|ALTER|TRUNCATE|EXEC|EXECUTE|UNION|DECLARE|CAST|CONVERT|SCRIPT|JAVASCRIPT)\\b",
                    Pattern.CASE_INSENSITIVE);

    /** 注释模式 - 防止SQL注释注入 */
    private static final Pattern COMMENT_PATTERN =
            Pattern.compile(
                    "(--|/\\*|\\*/|#|;\\s*DROP|;\\s*DELETE|;\\s*UPDATE|;\\s*INSERT)",
                    Pattern.CASE_INSENSITIVE);

    /**
     * 构建用户自定义WHERE条件
     *
     * @param dto 日志搜索DTO（其中的whereSqls可能已经被DTO转换器转换过）
     * @return WHERE条件字符串，如果没有条件则返回空字符串
     * @throws BusinessException 如果检测到潜在的SQL注入攻击
     */
    public String buildWhereConditions(LogSearchDTO dto) {
        if (dto.getWhereSqls() == null || dto.getWhereSqls().isEmpty()) {
            return "";
        }

        List<String> validConditions =
                dto.getWhereSqls().stream()
                        .filter(StringUtils::isNotBlank)
                        .map(String::trim)
                        .map(this::validateAndSanitizeCondition)
                        .collect(Collectors.toList());

        if (validConditions.isEmpty()) {
            return "";
        }

        // 单个条件：直接返回，不添加外层括号
        if (validConditions.size() == 1) {
            return validConditions.get(0);
        }

        // 多个条件：使用AND连接，并添加外层括号
        return validConditions.stream().collect(Collectors.joining(" AND ", "(", ")"));
    }

    /**
     * 验证和清理单个WHERE条件
     *
     * @param condition 原始WHERE条件
     * @return 验证后的WHERE条件
     * @throws BusinessException 如果检测到SQL注入风险
     */
    private String validateAndSanitizeCondition(String condition) {
        if (StringUtils.isBlank(condition)) {
            return condition;
        }

        String trimmedCondition = condition.trim();

        // 检测危险的SQL关键字
        if (DANGEROUS_SQL_PATTERN.matcher(trimmedCondition).find()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "WHERE条件包含危险的SQL关键字，可能存在SQL注入风险: " + condition);
        }

        // 检测SQL注释注入
        if (COMMENT_PATTERN.matcher(trimmedCondition).find()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "WHERE条件包含SQL注释或分号，可能存在SQL注入风险: " + condition);
        }

        // 检测连续的单引号（可能的字符串转义尝试）
        if (trimmedCondition.contains("'''")) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "WHERE条件包含可疑的引号模式，可能存在SQL注入风险: " + condition);
        }

        // 基础长度限制
        if (trimmedCondition.length() > 1000) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "WHERE条件过长，最大长度为1000字符: " + trimmedCondition.length());
        }

        return trimmedCondition;
    }
}
