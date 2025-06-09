package com.hinadt.miaocha.domain.converter;

import com.hinadt.miaocha.domain.dto.SqlHistoryResponseDTO.SqlHistoryItemDTO;
import com.hinadt.miaocha.domain.entity.SqlQueryHistory;
import com.hinadt.miaocha.domain.entity.User;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** SQL查询历史转换器 */
@Component
public class SqlQueryHistoryConverter implements Converter<SqlQueryHistory, SqlHistoryItemDTO> {

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public SqlQueryHistory toEntity(SqlHistoryItemDTO dto) {
        // 通常不需要从DTO转回实体，但为了满足接口要求实现
        SqlQueryHistory entity = new SqlQueryHistory();
        entity.setId(dto.getId());
        entity.setUserId(dto.getUserId());
        entity.setDatasourceId(dto.getDatasourceId());
        entity.setTableName(dto.getTableName());
        entity.setSqlQuery(dto.getSqlQuery());
        return entity;
    }

    @Override
    public SqlHistoryItemDTO toDto(SqlQueryHistory entity) {
        return convertToDto(entity, null);
    }

    /**
     * 将SqlQueryHistory实体转换为DTO，并设置用户邮箱
     *
     * @param entity SQL查询历史记录
     * @param user 用户信息，可以为null
     * @return DTO对象
     */
    public SqlHistoryItemDTO convertToDto(SqlQueryHistory entity, User user) {
        if (entity == null) {
            return null;
        }

        SqlHistoryItemDTO dto = new SqlHistoryItemDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setDatasourceId(entity.getDatasourceId());
        dto.setTableName(entity.getTableName());
        dto.setSqlQuery(entity.getSqlQuery());

        // 设置用户邮箱
        if (user != null) {
            dto.setUserEmail(user.getEmail());
        }

        // 判断是否有结果文件
        boolean hasResultFile =
                StringUtils.isNotBlank(entity.getResultFilePath())
                        && new File(entity.getResultFilePath()).exists();
        dto.setHasResultFile(hasResultFile);

        if (hasResultFile) {
            dto.setDownloadUrl("/api/sql/result/" + entity.getId());
        }

        // 格式化创建时间
        if (entity.getCreateTime() != null) {
            dto.setCreateTime(entity.getCreateTime().format(DATETIME_FORMATTER));
        }

        return dto;
    }

    /**
     * 批量转换历史记录
     *
     * @param historyList 历史记录列表
     * @param userMap 用户映射 (ID -> User)
     * @return DTO列表
     */
    public List<SqlHistoryItemDTO> convertToDtoList(
            List<SqlQueryHistory> historyList, Map<Long, User> userMap) {
        if (historyList == null || historyList.isEmpty()) {
            return Collections.emptyList();
        }

        return historyList.stream()
                .map(
                        history ->
                                convertToDto(
                                        history,
                                        userMap != null ? userMap.get(history.getUserId()) : null))
                .collect(Collectors.toList());
    }

    @Override
    public SqlQueryHistory updateEntity(SqlQueryHistory entity, SqlHistoryItemDTO dto) {
        // 通常不需要使用DTO更新实体，但为了满足接口要求实现
        if (entity != null && dto != null) {
            entity.setTableName(dto.getTableName());
            entity.setSqlQuery(dto.getSqlQuery());
        }
        return entity;
    }
}
