package com.hinadt.miaocha.domain.dto;

import java.util.List;

/**
 * LogSearchDTO装饰器 使用装饰器模式包装原始DTO，在传递给SQL构建器之前完成字段转换
 *
 * <p>设计原则： - 单一职责：只负责字段转换，不影响SQL构建逻辑 - 开闭原则：可以扩展新的转换器而不修改现有代码 - 依赖倒置：SQL构建器依赖抽象的DTO接口，不知道转换细节
 */
public class LogSearchDTODecorator extends LogSearchDTO {

    private final LogSearchDTO delegate;
    private final List<String> convertedFields;
    private final List<String> convertedWhereSqls;

    public LogSearchDTODecorator(
            LogSearchDTO original, List<String> convertedFields, List<String> convertedWhereSqls) {
        this.delegate = original;
        this.convertedFields = convertedFields;
        this.convertedWhereSqls = convertedWhereSqls;
    }

    // 重写需要转换的字段
    @Override
    public List<String> getFields() {
        return convertedFields != null ? convertedFields : delegate.getFields();
    }

    @Override
    public List<String> getWhereSqls() {
        return convertedWhereSqls != null ? convertedWhereSqls : delegate.getWhereSqls();
    }

    // 其他字段直接委托给原始对象
    @Override
    public Long getDatasourceId() {
        return delegate.getDatasourceId();
    }

    @Override
    public String getModule() {
        return delegate.getModule();
    }

    @Override
    public List<KeywordConditionDTO> getKeywordConditions() {
        return delegate.getKeywordConditions();
    }

    @Override
    public String getStartTime() {
        return delegate.getStartTime();
    }

    @Override
    public String getEndTime() {
        return delegate.getEndTime();
    }

    @Override
    public String getTimeRange() {
        return delegate.getTimeRange();
    }

    @Override
    public String getTimeGrouping() {
        return delegate.getTimeGrouping();
    }

    @Override
    public Integer getPageSize() {
        return delegate.getPageSize();
    }

    @Override
    public Integer getOffset() {
        return delegate.getOffset();
    }

    @Override
    public void setDatasourceId(Long datasourceId) {
        delegate.setDatasourceId(datasourceId);
    }

    @Override
    public void setModule(String module) {
        delegate.setModule(module);
    }

    @Override
    public void setKeywordConditions(List<KeywordConditionDTO> keywordConditions) {
        delegate.setKeywordConditions(keywordConditions);
    }

    @Override
    public void setWhereSqls(List<String> whereSqls) {
        delegate.setWhereSqls(whereSqls);
    }

    @Override
    public void setFields(List<String> fields) {
        delegate.setFields(fields);
    }

    @Override
    public void setStartTime(String startTime) {
        delegate.setStartTime(startTime);
    }

    @Override
    public void setEndTime(String endTime) {
        delegate.setEndTime(endTime);
    }

    @Override
    public void setTimeRange(String timeRange) {
        delegate.setTimeRange(timeRange);
    }

    @Override
    public void setTimeGrouping(String timeGrouping) {
        delegate.setTimeGrouping(timeGrouping);
    }

    @Override
    public void setPageSize(Integer pageSize) {
        delegate.setPageSize(pageSize);
    }

    @Override
    public void setOffset(Integer offset) {
        delegate.setOffset(offset);
    }

    /**
     * 获取原始DTO
     *
     * @return 原始DTO对象
     */
    public LogSearchDTO getOriginal() {
        return delegate;
    }
}
