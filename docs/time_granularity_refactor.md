# 时间分组智能计算使用手册 - 优化版

## 功能说明

秒查系统支持自动选择最佳时间分组间隔，支持毫秒级别精度，让数据可视化效果更佳。

## 基本用法

|     参数名称      |  设置值   |          说明           |
|---------------|--------|-----------------------|
| timeGrouping  | "auto" | 启用自动计算                |
| targetBuckets | 数字（可选） | 目标数据点数量，默认55（45-60范围） |
| startTime     | 时间字符串  | 查询开始时间                |
| endTime       | 时间字符串  | 查询结束时间                |

## 新增特性

### 🔥 毫秒级别精度支持

- 支持10ms、20ms、50ms、100ms、200ms、500ms间隔
- 适合高频率日志分析（每秒几万条日志）
- 特别适合短时间范围的精细分析

### 🎯 优化桶数量范围

- 默认目标桶数：55个（从50个优化）
- 建议桶数范围：45-60个（从5-50个优化）
- 最小桶数保证：45个（从5个提升）
- 最大桶数限制：10000个（保持不变）

## 自动计算效果表

| 查询时间范围 | 自动选择间隔 | 数据点数量 |  适合场景  |
|--------|--------|-------|--------|
| 6秒     | 每100毫秒 | 60个点  | 实时性能监控 |
| 30秒    | 每500毫秒 | 60个点  | 故障瞬间分析 |
| 最近2分钟  | 每2秒    | 60个点  | 系统响应监控 |
| 最近5分钟  | 每5秒    | 60个点  | 实时监控   |
| 最近30分钟 | 每30秒   | 60个点  | 短期监控   |
| 最近1小时  | 每1分钟   | 60个点  | 小时级分析  |
| 最近6小时  | 每10分钟  | 36个点  | 工作时段分析 |
| 最近1天   | 每30分钟  | 48个点  | 日常分析   |
| 最近1周   | 每3小时   | 56个点  | 周度趋势   |
| 最近1个月  | 每12小时  | 60个点  | 月度报表   |

## 精度控制表

| targetBuckets值 |  效果  |    6秒查询     |    1小时查询    |    1天查询    |
|----------------|------|-------------|-------------|------------|
| 45（最小值）        | 粗粒度  | 每130毫秒，45个点 | 每1.3分钟，45个点 | 每32分钟，45个点 |
| 55（默认）         | 标准精度 | 每100毫秒，55个点 | 每1分钟，60个点   | 每30分钟，48个点 |
| 60（建议最大）       | 高精度  | 每100毫秒，60个点 | 每1分钟，60个点   | 每24分钟，60个点 |

## 毫秒级别支持详情

| 时间间隔  |   适用场景    | 示例时间范围 |  预期桶数量  |
|-------|-----------|--------|---------|
| 10ms  | 极高频日志分析   | 0.5-1秒 | 50-100桶 |
| 20ms  | 高频性能监控    | 1-2秒   | 50-100桶 |
| 50ms  | 系统响应时间分析  | 2-5秒   | 40-100桶 |
| 100ms | 网络延迟监控    | 5-10秒  | 50-100桶 |
| 200ms | 用户交互响应分析  | 10-20秒 | 50-100桶 |
| 500ms | 短时间窗口业务监控 | 20-60秒 | 40-120桶 |

## 返回字段说明

|       字段名称        |   说明   |       示例值       |      更新说明       |
|-------------------|--------|-----------------|-----------------|
| timeUnit          | 时间单位   | millisecond     | 新增毫秒级别支持        |
| timeInterval      | 间隔数值   | 100             | 支持毫秒级数值（如100毫秒） |
| estimatedBuckets  | 预计数据点数 | 55              | 优化范围45-60，更准确预估 |
| actualBuckets     | 实际数据点数 | 52              | 与预估桶数差距更小       |
| calculationMethod | 计算方式   | AUTO_CALCULATED | 增强算法精度          |

## 使用场景对照表

|   场景   | timeGrouping | targetBuckets |       效果        | 时间精度 |
|--------|--------------|---------------|-----------------|------|
| 故障秒级分析 | auto         | 55            | 6秒查询，每100毫秒一个点  | 毫秒级  |
| 实时大屏监控 | auto         | 60            | 30秒查询，每500毫秒一个点 | 毫秒级  |
| 性能压测分析 | auto         | 50            | 2分钟压测，每2秒一个点    | 秒级   |
| 故障排查分析 | auto         | 55            | 1小时查询，每1分钟一个点   | 分钟级  |
| 日常运营报表 | auto         | 50            | 1天查询，每30分钟一个点   | 分钟级  |

## 性能优化特性

### 智能桶数量控制

- **建议范围提醒**：超过60个桶时会记录INFO日志提醒
- **最小桶数保证**：确保至少45个桶，避免图表过于稀疏
- **最大桶数保护**：超过10000个桶自动调整间隔

### 高精度计算

- **毫秒精度计算**：使用毫秒而非秒作为计算基准
- **精确间隔匹配**：更准确的标准间隔选择算法
- **预估准确性**：减少预估桶数与实际桶数的差异

## 迁移指南

|       原有配置        |       优化后配置       |       改进点       |
|-------------------|-------------------|-----------------|
| targetBuckets: 20 | targetBuckets: 45 | 满足最小桶数量要求       |
| targetBuckets: 50 | targetBuckets: 55 | 更好的默认体验         |
| 不支持短时间范围          | 自动毫秒级支持           | 6秒时间范围从6桶提升到55桶 |
| 桶数量差异较大           | 预估与实际接近           | 毫秒精度计算减少差异      |

## 基础示例

### 启用毫秒级自动计算

```json
{
  "timeGrouping": "auto",
  "targetBuckets": 55,
  "startTime": "2023-01-01 10:00:00",
  "endTime": "2023-01-01 10:00:06"
}
```

### 高精度短时间分析

```json
{
  "timeGrouping": "auto", 
  "targetBuckets": 60,
  "startTime": "2023-01-01 10:00:00",
  "endTime": "2023-01-01 10:00:30"
}
```

### 保持原有固定间隔

```json
{
  "timeGrouping": "minute"
}
```

## 算法升级说明

1. **毫秒精度计算**：从秒级精度升级到毫秒级精度
2. **桶数量优化**：目标范围从5-50优化为45-60
3. **标准间隔扩展**：新增6个毫秒级标准间隔
4. **智能调整**：最小桶数保证45个，避免图表过于稀疏
5. **性能保护**：超过建议范围会记录提醒日志

## 兼容性说明

- ✅ 向后兼容：原有API完全兼容
- ✅ 数据格式：返回字段格式保持一致
- ✅ SQL支持：timeUnit字段增加millisecond支持
- ✅ 前端适配：需要支持millisecond时间单位的图表渲染

## 时间范围示例

| 时间范围描述 |      startTime      |       endTime       | 自动计算结果 |
|--------|---------------------|---------------------|--------|
| 最近1小时  | 2023-01-01 10:00:00 | 2023-01-01 11:00:00 | 每1分钟   |
| 今天全天   | 2023-01-01 00:00:00 | 2023-01-01 23:59:59 | 每30分钟  |
| 最近一周   | 2023-01-01 00:00:00 | 2023-01-07 23:59:59 | 每3小时   |

## 迁移对照表

|          旧方式           |         新方式          |    优势    |
|------------------------|----------------------|----------|
| timeGrouping: "minute" | timeGrouping: "auto" | 自动适应时间范围 |
| timeGrouping: "hour"   | timeGrouping: "auto" | 数据点数量更合理 |
| timeGrouping: "day"    | timeGrouping: "auto" | 可视化效果更佳  |

## 系统限制

|  限制项  |   数值    |    说明    |
|-------|---------|----------|
| 最大数据点 | 10,000个 | 超过自动调整间隔 |
| 最小数据点 | 5个      | 保证图表可显示  |
| 默认精度  | 50个点    | 平衡性能与效果  |

