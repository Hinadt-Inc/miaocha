package com.hina.log.service.sql.builder.condition;

import com.hina.log.dto.LogSearchDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 关键字MATCH_ANY条件构建器 - 已弃用
 * 处理单个关键字和多个关键字OR关系的情况
 * 
 * @deprecated 此构建器已弃用，但保留以供参考。
 *             原因：MATCH_ANY 在处理包含连字符（如
 *             'cddda693-7ee6-45ac-97ce-e03db01e8e22'）的文本时会被分词器自动分开，
 *             导致匹配不准确或错误。比如输入 'cddda693-7ee6-45ac-97ce-e03db01e8e22' 会以 -
 *             进行分割进行匹配。
 *             请使用 {@link KeywordOrConditionBuilder} 作为替代，它使用 OR 操作符结合
 *             MATCH_ALL 条件来替代 MATCH_ANY，
 *             形如: (message MATCH_ALL 'term1' OR message MATCH_ALL
 *             'term2')，避免分词问题。
 */
@Component
@Order(30) // 降低优先级，确保新的条件构建器优先执行
@Deprecated(since = "1.0", forRemoval = false)
public class KeywordMatchAnyConditionBuilder implements SearchConditionBuilder {

    @Override
    public boolean supports(LogSearchDTO dto) {
        // 为避免实际使用，始终返回 false
        return false;

        /*
         * 原始判断逻辑保留以供参考
         * if (StringUtils.isBlank(dto.getKeyword())) {
         * return false;
         * }
         * String keyword = dto.getKeyword().trim();
         * // 排除复杂表达式和AND关系的表达式
         * return !keyword.contains(" && ") && !keyword.contains("(") &&
         * !keyword.contains(")");
         */
    }

    @Override
    public String buildCondition(LogSearchDTO dto) {
        // 即使方法被调用，也不生成任何条件
        return "";

        /*
         * 原始实现保留以供参考
         * if (!supports(dto)) {
         * return "";
         * }
         * 
         * String keyword = dto.getKeyword().trim();
         * StringBuilder condition = new StringBuilder();
         * 
         * if (keyword.contains(" || ")) {
         * // 处理OR关系的关键字
         * String[] keywords = keyword.split(" \\|\\| ");
         * StringBuilder matchAnyClause = new StringBuilder();
         * for (int i = 0; i < keywords.length; i++) {
         * String key = keywords[i].trim().replaceAll("^'|'$", ""); // 去除可能的引号
         * if (StringUtils.isNotBlank(key)) {
         * if (i > 0) {
         * matchAnyClause.append(" ");
         * }
         * matchAnyClause.append(key);
         * }
         * }
         * if (matchAnyClause.length() > 0) {
         * condition.append("message MATCH_ANY '").append(matchAnyClause).append("'");
         * }
         * } else {
         * // 处理单个关键字
         * String key = keyword.replaceAll("^'|'$", ""); // 去除可能的引号
         * if (StringUtils.isNotBlank(key)) {
         * condition.append("message MATCH_ANY '").append(key).append("'");
         * }
         * }
         * 
         * return condition.toString();
         */
    }
}