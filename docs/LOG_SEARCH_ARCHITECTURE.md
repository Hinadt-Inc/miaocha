# 秒查日志查询系统深度架构分析

## 一、LogSearchServiceImpl调用的核心类体系

### 1.1 完整依赖关系图

```
LogSearchServiceImpl (核心服务层)
├── 验证层 (Validation Layer)
│   ├── LogSearchValidator
│   │   ├── DatasourceMapper - 数据源验证
│   │   └── UserMapper - 用户验证
│   └── QueryConfigValidationService
│       └── ModuleInfoService - 模块配置验证
├── 模板层 (Template Layer)
│   └── LogSearchTemplate
│       ├── TimeRangeProcessor - 时间范围处理
│       ├── LogSearchDTOConverter - DTO转换
│       └── QueryConfigValidationService - 配置验证
├── 执行器层 (Executor Layer)
│   ├── DetailSearchExecutor
│   │   ├── LogSqlBuilder - SQL构建
│   │   ├── ResultProcessor - 结果处理
│   │   └── JdbcQueryExecutor - 查询执行
│   ├── HistogramSearchExecutor
│   │   ├── LogSqlBuilder - SQL构建
│   │   ├── ResultProcessor - 结果处理
│   │   ├── TimeRangeProcessor - 时间处理
│   │   └── JdbcQueryExecutor - 查询执行
│   └── FieldDistributionSearchExecutor
│       ├── LogSqlBuilder - SQL构建
│       ├── LogSearchDTOConverter - 字段转换
│       └── JdbcQueryExecutor - 查询执行
├── 数据访问层 (Data Access Layer)
│   ├── DatabaseMetadataServiceFactory
│   │   ├── DorisMetadataService - Doris特有功能
│   │   ├── MySQLMetadataService - MySQL支持
│   │   └── PostgreSQLMetadataService - PostgreSQL支持
│   └── JdbcQueryExecutor - 统一数据库操作
└── 业务服务层 (Business Service Layer)
    └── ModuleInfoService - 模块业务逻辑
```

### 1.2 核心依赖类的详细职责

#### 验证层类
- **LogSearchValidator**: 
  - 验证用户存在性 (`validateUser`)
  - 验证数据源有效性 (`validateAndGetDatasource`)
  - 验证分页参数合法性 (`validatePaginationParams`)
  - 验证字段列表完整性 (`validateFields`)

- **QueryConfigValidationService**:
  - 验证模块查询配置完整性 (`validateAndGetQueryConfig`)
  - 验证关键字字段权限 (`validateKeywordFieldPermissions`)
  - 获取字段搜索方法映射 (`getFieldSearchMethodMap`)
  - 获取配置的时间字段 (`getTimeField`)

#### 模板层类
- **LogSearchTemplate**:
  - 统一执行流程控制 (模板方法模式)
  - 时间范围预处理 (`timeRangeProcessor.processTimeRange`)
  - DTO转换协调 (`dtoConverter.convert`)
  - 数据库连接管理
  - 执行时间统计

#### 执行器层类
- **DetailSearchExecutor**:
  - 详情查询和总数查询并行执行
  - 使用CompletableFuture提高性能
  - 结果处理和异常转换

- **HistogramSearchExecutor**:
  - 时间颗粒度自动计算
  - 时间分布查询优化
  - 分布结果处理

- **FieldDistributionSearchExecutor**:
  - 字段分布的两层查询优化
  - TOPN函数应用
  - 点语法字段转换

#### 数据访问层类
- **DatabaseMetadataServiceFactory**:
  - 根据数据库类型返回对应的元数据服务
  - 支持Doris、MySQL、PostgreSQL等

- **JdbcQueryExecutor**:
  - 统一的数据库连接获取
  - SQL执行和结果集处理
  - 连接池管理

#### 业务服务层类
- **ModuleInfoService**:
  - 模块名到表名的映射 (`getTableNameByModule`)
  - 查询配置获取 (`getQueryConfigByModule`)
  - 模块配置管理

## 二、LogSqlBuilder拼接SQL的完整规则体系

### 2.1 SQL构建器架构设计

LogSqlBuilder采用**门面模式 + 专用构建器**架构：

```
LogSqlBuilder (门面类)
├── DistributionSqlBuilder - 时间分布SQL专用构建器
├── DetailSqlBuilder - 详情查询SQL专用构建器
├── FieldDistributionSqlBuilder - 字段分布SQL专用构建器
└── KeywordConditionBuilder - 关键字条件专用构建器
```

### 2.2 关键字搜索的配置驱动机制

#### 配置驱动的搜索方法选择
系统支持按模块、按字段配置不同的搜索方法：

```json
{
  "timeField": "log_time",
  "keywordFields": [
    {
      "fieldName": "message",
      "searchMethod": "MATCH_PHRASE"
    },
    {
      "fieldName": "level", 
      "searchMethod": "LIKE"
    },
    {
      "fieldName": "tags",
      "searchMethod": "MATCH_ANY"
    },
    {
      "fieldName": "keywords",
      "searchMethod": "MATCH_ALL"
    }
  ]
}
```

#### 四种搜索方法的SQL生成规则

**1. LIKE搜索方法**
- 单条件：`message LIKE '%error%'`
- OR表达式：`'error' || 'warning'` → `message LIKE '%error%' OR message LIKE '%warning%'`
- AND表达式：`'java' && 'exception'` → `message LIKE '%java%' AND message LIKE '%exception%'`

**2. MATCH_PHRASE搜索方法**
- 单条件：`message MATCH_PHRASE 'NullPointerException'`
- OR表达式：`'OutOfMemoryError' || 'StackOverflowError'` → `message MATCH_PHRASE 'OutOfMemoryError' OR message MATCH_PHRASE 'StackOverflowError'`
- AND表达式：`'error' && 'critical'` → `message MATCH_PHRASE 'error' AND message MATCH_PHRASE 'critical'`

**3. MATCH_ANY搜索方法**
- 单条件：`tags MATCH_ANY 'critical'`
- OR表达式：`'error' || 'warning'` → `tags MATCH_ANY 'error' OR tags MATCH_ANY 'warning'`
- AND表达式：`'production' && 'critical'` → `tags MATCH_ANY 'production' AND tags MATCH_ANY 'critical'`

**4. MATCH_ALL搜索方法**
- 单条件：`keywords MATCH_ALL 'payment success'`
- 复杂表达式：`('payment' || 'order') && 'success'` → `( keywords MATCH_ALL 'payment' OR keywords MATCH_ALL 'order' ) AND keywords MATCH_ALL 'success'`

#### 复杂表达式解析机制

**支持的表达式语法**：
- 关键词引用：`'keyword'` 或 `"keyword"`
- OR运算符：`||`
- AND运算符：`&&` 
- 括号分组：`()`
- 嵌套表达式：`(('a' || 'b') && 'c')`

**解析算法**：
```java
public String parseKeywordExpression(String expression) {
    // 1. 词法分析：提取关键词和运算符
    List<Token> tokens = tokenize(expression);
    
    // 2. 语法分析：构建抽象语法树，处理运算符优先级
    ExpressionNode ast = parse(tokens);
    
    // 3. SQL生成：遍历AST，调用对应搜索方法生成SQL
    return generateSQL(ast);
}
```

### 2.3 WHERE条件搜索处理规则

**条件构建优先级**：
1. **时间条件** (最高优先级)：`WHERE log_time >= 'startTime' AND log_time < 'endTime'`
2. **关键字条件**：通过KeywordConditionBuilder处理，支持多字段多搜索方法
3. **用户WHERE条件**：直接拼接用户提供的whereSqls列表
4. **条件组合**：使用AND连接各类条件

**WHERE条件SQL结构**：
```sql
WHERE log_time >= '2023-01-01 00:00:00' AND log_time < '2023-01-02 00:00:00'
  AND (message MATCH_PHRASE 'error' AND message MATCH_PHRASE 'critical')  -- 关键字条件
  AND (level = 'ERROR')                                                    -- 用户WHERE条件1
  AND (service_name = 'order-service')                                     -- 用户WHERE条件2
```

**多字段关键字条件处理**：
```sql
-- 多字段条件会自动添加外层括号
((message LIKE '%error%') AND (level MATCH_PHRASE 'ERROR') AND (service MATCH_ALL 'order-service'))
```

### 2.4 完整SQL模板结构

**详情查询SQL模板**：
```sql
SELECT [fields] FROM table_name 
WHERE log_time >= 'startTime' AND log_time < 'endTime'
  [AND (keyword_conditions)]
  [AND (where_condition1)]
  [AND (where_condition2)]
ORDER BY log_time DESC 
LIMIT limit OFFSET offset
```

**时间分布查询SQL模板**：
```sql
SELECT [time_bucket_expression] as time_bucket, COUNT(*) as count
FROM table_name
WHERE log_time >= 'startTime' AND log_time < 'endTime'
  [AND (keyword_conditions)]
  [AND (where_conditions)]
GROUP BY time_bucket
ORDER BY time_bucket ASC
```

**字段分布查询SQL模板**：
```sql
SELECT TOPN(field1, 5) AS 'field1', TOPN(field2, 5) AS 'field2'
FROM (
    SELECT * FROM table_name
    WHERE log_time >= 'startTime' AND log_time < 'endTime'
      [AND (keyword_conditions)]
      [AND (where_conditions)]
    ORDER BY log_time DESC
    LIMIT 5000 OFFSET 0
) AS sample_data
```

# 日志查询系统架构

## 系统架构

秒查日志查询系统采用分层架构，支持多数据源日志查询和毫秒级时间分组。

### 核心组件

```
LogSearchServiceImpl
├── LogSqlBuilder (SQL构建)
├── SearchConditionManager (搜索条件)
├── TimeRangeProcessor (时间处理)
├── VariantFieldConverter (字段转换)
└── DatabaseMetadataServiceFactory (数据库适配)
```

## SQL构建规则

### 查询结构模板

```sql
SELECT [fields] FROM table_name 
WHERE log_time >= 'startTime' AND log_time < 'endTime'
  [AND keyword_conditions] [AND where_conditions]
ORDER BY log_time DESC LIMIT limit OFFSET offset
```

### 关键字搜索语法

采用**配置驱动**的搜索方法选择机制。不同模块、不同字段可配置不同搜索方法：

| 搜索方法         | 单条件示例                     | 复杂表达式示例                                                     |
| ---------------- | ------------------------------ | ------------------------------------------------------------------ |
| **LIKE**         | `message LIKE '%error%'`       | `message LIKE '%error%' OR message LIKE '%warning%'`               |
| **MATCH_PHRASE** | `message MATCH_PHRASE 'error'` | `message MATCH_PHRASE 'error' AND message MATCH_PHRASE 'critical'` |
| **MATCH_ANY**    | `tags MATCH_ANY 'critical'`    | `tags MATCH_ANY 'error' OR tags MATCH_ANY 'warning'`               |
| **MATCH_ALL**    | `keywords MATCH_ALL 'payment'` | `keywords MATCH_ALL 'payment' AND keywords MATCH_ALL 'success'`    |

**表达式解析**：支持 `'term1' || 'term2'` (OR)、`'term1' && 'term2'` (AND)、括号嵌套

## 时间分组机制

### 支持间隔

22种标准间隔，毫秒级精度：

| 类型 | 间隔值                       |
| ---- | ---------------------------- |
| 毫秒 | 10, 20, 50, 100, 200, 500 ms |
| 秒   | 1, 2, 5, 10, 15, 30 s        |
| 分钟 | 1, 2, 5, 10, 15, 30 min      |
| 小时 | 1, 2, 3, 6, 12 h             |
| 天   | 1 d                          |

**自动计算**: 目标桶数55 (45-60)，最大10000，基于 Kibana 算法

### SQL语法

```sql
-- 标准间隔
SELECT date_trunc(log_time, 'minute') as time_bucket, COUNT(*) 
FROM table_name GROUP BY time_bucket

-- 毫秒级自定义间隔
SELECT FLOOR(MILLISECOND_TIMESTAMP(log_time) / intervalMillis) * intervalMillis as time_bucket
FROM table_name GROUP BY time_bucket
```

## 查询类型

| 类型         | 实现方式                              |
| ------------ | ------------------------------------- |
| **详情查询** | `ORDER BY log_time DESC LIMIT offset` |
| **时间分布** | 按计算间隔分组统计                    |
| **字段分布** | 两层查询：内层5000样本 + 外层TOPN(5)  |

## 字段转换 (Doris Variant)

点语法自动转换为括号语法：

| 输入                  | 输出                        |
| --------------------- | --------------------------- |
| `message.logId`       | `message['logId']`          |
| `message.marker.data` | `message['marker']['data']` |
| `log.user_info.name`  | `log['user_info']['name']`  |

**转换范围**: SELECT(带别名)、WHERE(直接替换)、TOPN(字段转换)  
**安全特性**: 引号保护、Unicode支持、语法验证

## 数据库支持

| 数据库         | 支持特性                      |
| -------------- | ----------------------------- |
| **Doris**      | Variant字段转换、时间函数适配 |
| **MySQL**      | 基础SQL语法                   |
| **PostgreSQL** | 时间函数适配                  |
| **ClickHouse** | 高性能查询优化                |

## 技术规范

### 核心配置

| 参数                            | 默认值 | 说明               |
| ------------------------------- | ------ | ------------------ |
| `search.max.result.size`        | 10000  | 单次查询最大结果数 |
| `search.sample.size`            | 5000   | 字段分布采样大小   |
| `search.query.timeout`          | 30s    | 查询超时时间       |
| `time.grouping.default.buckets` | 55     | 默认目标桶数       |

### 错误码

| 类型     | 错误码 | 说明               |
| -------- | ------ | ------------------ |
| 参数错误 | 400xx  | 请求参数不合法     |
| 权限错误 | 403xx  | 数据源访问权限不足 |
| 查询错误 | 500xx  | SQL执行失败        |
| 超时错误 | 504xx  | 查询执行超时       |

### 扩展接口

**搜索条件扩展**: 实现 `SearchConditionBuilder` 接口，使用 `@Order` 控制优先级  
**数据库适配**: 实现 `DatabaseMetadataService` 接口，返回对应数据库类型

## 三、时间字段处理的完整机制体系

### 3.1 时间范围预处理机制

**TimeRangeProcessor核心功能**：
- 预定义时间范围解析
- 自定义时间范围验证
- 时间格式标准化

**支持的预定义时间范围**：
```java
// 分钟级快捷范围
last_5m, last_15m, last_30m
// 小时级快捷范围  
last_1h, last_6h, last_12h
// 天级快捷范围
today, yesterday, last_24h, last_7d, last_30d
// 特殊范围
this_week, this_month, last_week, last_month
```

**时间格式支持**：
```java
// 支持毫秒精度
"2023-01-01 10:00:00.123"
// 支持标准格式
"2023-01-01 10:00:00"
// 支持ISO格式
"2023-01-01T10:00:00Z"
```

### 3.2 时间颗粒度计算的Kibana算法实现

#### 22种标准间隔体系
```java
// 毫秒级别间隔 (6种)
10ms, 20ms, 50ms, 100ms, 200ms, 500ms

// 秒级别间隔 (6种)
1s, 2s, 5s, 10s, 15s, 30s

// 分钟级别间隔 (6种)  
1min, 2min, 5min, 10min, 15min, 30min

// 小时级别间隔 (5种)
1h, 2h, 3h, 6h, 12h

// 天级别间隔 (5种)
1d, 2d, 3d, 7d, 14d, 30d
```

#### 核心算法实现
```java
public TimeGranularityResult calculateOptimalGranularity(
        String startTime, String endTime, String userSpecifiedUnit, Integer targetBuckets) {
    
    // 1. 时间范围计算 (毫秒精度)
    Duration timeRange = Duration.between(parseDateTime(startTime), parseDateTime(endTime));
    long timeRangeMillis = timeRange.toMillis();
    
    // 2. 原始间隔计算
    int actualTargetBuckets = targetBuckets != null ? targetBuckets : DEFAULT_TARGET_BUCKETS(55);
    long rawIntervalMillis = timeRangeMillis / actualTargetBuckets;
    
    // 3. 找到最接近的标准间隔
    TimeInterval optimalInterval = findClosestStandardIntervalByMillis(rawIntervalMillis);
    
    // 4. 计算实际桶数量
    long actualBuckets = timeRangeMillis / optimalInterval.duration.toMillis();
    
    // 5. 性能保护机制
    if (actualBuckets > MAX_BUCKETS(10000)) {
        optimalInterval = findIntervalForMaxBucketsByMillis(timeRangeMillis, MAX_BUCKETS);
    }
    if (actualBuckets < MIN_BUCKETS(45)) {
        optimalInterval = findIntervalForMinBucketsByMillis(timeRangeMillis, MIN_BUCKETS);
    }
    
    return buildResult(optimalInterval, actualBuckets);
}
```

#### 桶数量控制策略
- **默认目标桶数**: 55个 (基于Kibana最佳实践)
- **建议桶数范围**: 45-60个 (平衡展示效果和性能)
- **最大桶数限制**: 10000个 (性能保护上限)
- **最小桶数保证**: 45个 (确保基本展示效果)

### 3.3 时间字段SQL拼接的三种方式

#### 1. 标准间隔SQL (使用date_trunc函数)
```sql
-- 分钟级标准间隔
SELECT date_trunc(log_time, 'minute') as time_bucket, COUNT(*) as count
FROM table_name 
WHERE log_time >= '2023-01-01 10:00:00' AND log_time < '2023-01-01 11:00:00'
GROUP BY time_bucket
ORDER BY time_bucket ASC

-- 小时级标准间隔  
SELECT date_trunc(log_time, 'hour') as time_bucket, COUNT(*) as count
FROM table_name
WHERE log_time >= '2023-01-01 00:00:00' AND log_time < '2023-01-02 00:00:00'
GROUP BY time_bucket
ORDER BY time_bucket ASC
```

#### 2. 自定义间隔SQL (使用UNIX_TIMESTAMP函数)
```sql
-- 15分钟自定义间隔 (900秒)
SELECT FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(log_time) / 900) * 900) as time_bucket, COUNT(*) as count
FROM table_name
WHERE log_time >= '2023-01-01 10:00:00' AND log_time < '2023-01-01 12:00:00'
GROUP BY time_bucket
ORDER BY time_bucket ASC

-- 2小时自定义间隔 (7200秒)
SELECT FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(log_time) / 7200) * 7200) as time_bucket, COUNT(*) as count
FROM table_name  
WHERE log_time >= '2023-01-01 00:00:00' AND log_time < '2023-01-08 00:00:00'
GROUP BY time_bucket
ORDER BY time_bucket ASC
```

#### 3. 毫秒级精度SQL (使用MILLISECOND_TIMESTAMP函数)
```sql
-- 100毫秒间隔
SELECT FLOOR(MILLISECOND_TIMESTAMP(log_time) / 100) * 100 as time_bucket, COUNT(*) as count
FROM table_name
WHERE log_time >= '2023-01-01 10:00:00.000' AND log_time < '2023-01-01 10:00:10.000'
GROUP BY time_bucket
ORDER BY time_bucket ASC

-- 500毫秒间隔
SELECT FLOOR(MILLISECOND_TIMESTAMP(log_time) / 500) * 500 as time_bucket, COUNT(*) as count  
FROM table_name
WHERE log_time >= '2023-01-01 10:00:00.000' AND log_time < '2023-01-01 10:01:00.000'
GROUP BY time_bucket
ORDER BY time_bucket ASC
```

#### 时间字段配置获取机制
```java
private String getTimeField(String module) {
    try {
        // 从模块配置中获取时间字段
        return queryConfigValidationService.getTimeField(module);
    } catch (Exception e) {
        // 如果配置未找到，返回默认的log_time字段以保持兼容性
        return "log_time";
    }
}
```

## 四、分布查询和直方图查询的高级逻辑

### 4.1 时间分布查询(HistogramSearchExecutor)的完整执行流程

#### 执行步骤详解
```java
@Override
public LogHistogramResultDTO execute(SearchContext context) throws LogQueryException {
    LogSearchDTO dto = context.getDto();
    String tableName = context.getTableName();
    Connection conn = context.getConnection();

    // 1. 计算最优时间颗粒度
    TimeGranularityCalculator.TimeGranularityResult granularityResult =
            timeRangeProcessor.calculateOptimalTimeGranularity(dto, dto.getTargetBuckets());

    // 2. 构建分布统计查询SQL
    String distributionSql = logSqlBuilder.buildDistributionSqlWithInterval(
            dto, tableName, granularityResult.getTimeUnit(), granularityResult.getInterval());

    // 3. 异步执行查询 (性能优化)
    CompletableFuture<QueryResult> distributionFuture = executeQueryAsync(
            conn, distributionSql, ErrorCode.LOG_HISTOGRAM_QUERY_FAILED, "HistogramQuery");

    // 4. 处理查询结果
    QueryResult distributionQueryResult = distributionFuture.get();
    resultProcessor.processDistributionResult(distributionQueryResult, result);

    // 5. 设置时间颗粒度信息
    setGranularityInfo(result, granularityResult);

    return result;
}
```

#### 时间颗粒度自动选择示例
```java
// 示例1: 1小时时间范围 → 1分钟间隔
时间范围: 2023-01-01 10:00:00 到 2023-01-01 11:00:00 (3600秒)
目标桶数: 50
计算间隔: 3600 ÷ 50 = 72秒 → 选择最接近的 1分钟(60秒)
实际桶数: 3600 ÷ 60 = 60桶

// 示例2: 24小时时间范围 → 30分钟间隔  
时间范围: 2023-01-01 00:00:00 到 2023-01-02 00:00:00 (86400秒)
目标桶数: 50
计算间隔: 86400 ÷ 50 = 1728秒 ≈ 29分钟 → 选择最接近的 30分钟(1800秒)
实际桶数: 86400 ÷ 1800 = 48桶

// 示例3: 7天时间范围 → 3小时间隔
时间范围: 2023-01-01 00:00:00 到 2023-01-08 00:00:00 (604800秒)  
目标桶数: 50
计算间隔: 604800 ÷ 50 = 12096秒 ≈ 3.36小时 → 选择最接近的 3小时(10800秒)
实际桶数: 604800 ÷ 10800 = 56桶
```

### 4.2 字段分布查询(FieldDistributionSearchExecutor)的两层优化策略

#### 两层查询架构的设计原理
```sql
-- 完整的两层查询结构
SELECT TOPN(field1, 5) AS 'field1',
       TOPN(field2, 5) AS 'field2', 
       TOPN(field3, 5) AS 'field3'
FROM (
    -- 内层查询：性能优化采样
    SELECT * FROM table_name
    WHERE log_time >= '2023-01-01 00:00:00' 
      AND log_time < '2023-01-02 00:00:00'
      AND (message MATCH_PHRASE 'error')  -- 关键字条件
      AND (level = 'ERROR')               -- WHERE条件
    ORDER BY log_time DESC
    LIMIT 5000 OFFSET 0                   -- 采样大小限制
) AS sample_data
```

#### 性能优化设计思路
1. **内层查询优化**：
   - 限制采样数据量为5000条，避免全表扫描
   - 按时间倒序排序，优先获取最新数据
   - 应用所有过滤条件，确保采样数据的相关性

2. **外层TOPN优化**：
   - 在采样数据基础上计算Top5分布
   - 利用Doris TOPN函数的高性能特性
   - 支持多字段并行统计

3. **字段转换处理**：
   - 支持Doris Variant字段的点语法转换
   - TOPN字段转换：`business.region` → `business['region']`
   - 原始字段别名：保持用户输入的点语法作为别名

#### 字段分布查询执行流程
```java
@Override
public LogFieldDistributionResultDTO execute(SearchContext context) throws LogQueryException {
    LogSearchDTO dto = context.getDto();
    String tableName = context.getTableName();
    Connection conn = context.getConnection();

    // 1. 转换fields中的点语法为括号语法（用于TOPN函数）
    List<String> convertedTopnFields = dto.getFields().stream()
            .map(dtoConverter::convertTopnField)
            .collect(Collectors.toList());

    // 2. 构建字段分布查询SQL
    String fieldDistributionSql = logSqlBuilder.buildFieldDistributionSql(
            dto, tableName, convertedTopnFields, dto.getFields(), 5);

    // 3. 异步执行查询
    CompletableFuture<QueryResult> fieldDistributionFuture = executeQueryAsync(
            conn, fieldDistributionSql, ErrorCode.LOG_FIELD_DISTRIBUTION_QUERY_FAILED,
            "FieldDistributionQuery");

    // 4. 处理分布结果
    QueryResult distributionQueryResult = fieldDistributionFuture.get();
    processFieldDistributionResult(distributionQueryResult, result);

    return result;
}
```

### 4.3 并行查询优化机制

#### DetailSearchExecutor的并行查询实现
```java
// 详情查询和总数查询并行执行
CompletableFuture<QueryResult> detailFuture = executeQueryAsync(
        conn, detailSql, ErrorCode.LOG_DETAIL_QUERY_FAILED, "DetailQuery");
CompletableFuture<QueryResult> countFuture = executeQueryAsync(
        conn, countSql, ErrorCode.LOG_COUNT_QUERY_FAILED, "CountQuery");

// 等待两个查询完成
QueryResult detailQueryResult = detailFuture.get();
QueryResult countQueryResult = countFuture.get();
```

#### 异步查询基础设施
```java
protected CompletableFuture<QueryResult> executeQueryAsync(
        Connection conn, String sql, ErrorCode errorCode, String queryType) {
    return CompletableFuture.supplyAsync(() -> {
        try {
            return jdbcQueryExecutor.executeQuery(conn, sql);
        } catch (SQLException e) {
            throw new LogQueryException(errorCode, queryType + "执行失败: " + e.getMessage());
        }
    }, logQueryExecutor);
}
```

## 五、sql.builder.condition包的完整架构分析

### 5.1 条件构建器架构演进历史

#### 当前架构 (V2.0) - 配置驱动设计
```java
KeywordConditionBuilder (@Order(5) - 当前版本)
├── QueryConfigValidationService - 配置验证和权限检查
├── SearchMethod枚举 - 四种搜索方法的统一实现
└── FieldExpressionParser - 复杂表达式解析引擎
```

#### 已废弃的架构 (V1.0) - 硬编码实现
```java
// 这些类已被删除，仅作为架构演进参考
SearchConditionManager (已废弃)
├── KeywordPhraseConditionBuilder (@Order(5))
├── LikeConditionBuilder (@Order(10))  
├── KeywordFieldConditionBuilder (@Order(15))
└── WhereConditionBuilder (@Order(20))
```

### 5.2 SearchMethod枚举的详细实现机制

#### 枚举设计模式
```java
public enum SearchMethod implements SearchMethodHandler {
    LIKE("LIKE") {
        @Override
        public String buildSingleCondition(String fieldName, String keyword) {
            return fieldName + " LIKE '%" + keyword + "%'";
        }
    },
    
    MATCH_PHRASE("MATCH_PHRASE") {
        @Override  
        public String buildSingleCondition(String fieldName, String keyword) {
            return fieldName + " MATCH_PHRASE '" + keyword + "'";
        }
    },
    
    MATCH_ANY("MATCH_ANY") {
        @Override
        public String buildSingleCondition(String fieldName, String keyword) {
            return fieldName + " MATCH_ANY '" + keyword + "'"; 
        }
    },
    
    MATCH_ALL("MATCH_ALL") {
        @Override
        public String buildSingleCondition(String fieldName, String keyword) {
            return fieldName + " MATCH_ALL '" + keyword + "'";
        }
    }
}
```

#### 表达式解析统一接口
```java
@Override
public String parseExpression(String fieldName, String expression) {
    if (expression == null || expression.trim().isEmpty()) {
        return "";
    }
    
    // 使用统一的表达式解析器
    FieldExpressionParser parser = new FieldExpressionParser(fieldName, this);
    return parser.parseKeywordExpression(expression);
}
```

### 5.3 MATCH搜索方法的详细处理逻辑

#### MATCH_PHRASE - 精确短语匹配
**适用场景**：错误信息、异常堆栈、精确日志内容匹配

**处理逻辑**：
```java
// 单条件处理
输入: fieldName="message", keyword="NullPointerException"
输出: "message MATCH_PHRASE 'NullPointerException'"

// OR表达式处理  
输入: fieldName="message", expression="'OutOfMemoryError' || 'StackOverflowError'"
解析过程:
1. 词法分析: ['OutOfMemoryError'] [||] ['StackOverflowError']
2. 语法分析: OR(OutOfMemoryError, StackOverflowError)
3. SQL生成: "message MATCH_PHRASE 'OutOfMemoryError' OR message MATCH_PHRASE 'StackOverflowError'"

// AND表达式处理
输入: fieldName="message", expression="'error' && 'critical'"
输出: "message MATCH_PHRASE 'error' AND message MATCH_PHRASE 'critical'"

// 复杂嵌套表达式处理
输入: fieldName="message", expression="('OutOfMemoryError' || 'StackOverflowError') && 'critical'"
输出: "( message MATCH_PHRASE 'OutOfMemoryError' OR message MATCH_PHRASE 'StackOverflowError' ) AND message MATCH_PHRASE 'critical'"
```

#### MATCH_ANY - 任意词匹配
**适用场景**：标签搜索、分类匹配、多值字段查询

**处理逻辑**：
```java
// 单条件处理 - 支持多词匹配
输入: fieldName="tags", keyword="critical urgent"
输出: "tags MATCH_ANY 'critical urgent'"

// 表达式处理
输入: fieldName="tags", expression="'production' && 'critical'" 
输出: "tags MATCH_ANY 'production' AND tags MATCH_ANY 'critical'"

// 业务场景示例
输入: fieldName="environment_tags", expression="'production' || 'staging'"
输出: "environment_tags MATCH_ANY 'production' OR environment_tags MATCH_ANY 'staging'"
```

#### MATCH_ALL - 全词匹配
**适用场景**：关键词搜索、多条件必须匹配、复合查询

**处理逻辑**：
```java
// 单条件处理 - 所有词都必须匹配
输入: fieldName="keywords", keyword="payment success order"
输出: "keywords MATCH_ALL 'payment success order'"

// 复杂表达式处理
输入: fieldName="keywords", expression="('payment' || 'order') && 'success'"
解析过程:
1. 词法分析: [('payment' || 'order')] [&&] ['success']
2. 语法分析: AND(OR(payment, order), success)  
3. SQL生成: "( keywords MATCH_ALL 'payment' OR keywords MATCH_ALL 'order' ) AND keywords MATCH_ALL 'success'"

// 业务场景示例
输入: fieldName="business_keywords", expression="'payment' && 'completed' && 'notification'"
输出: "business_keywords MATCH_ALL 'payment' AND business_keywords MATCH_ALL 'completed' AND business_keywords MATCH_ALL 'notification'"
```

### 5.4 FieldExpressionParser的核心解析算法

#### 词法分析器 (Tokenizer)
```java
private List<Token> tokenize(String expression) {
    List<Token> tokens = new ArrayList<>();
    StringBuilder currentToken = new StringBuilder();
    boolean inQuote = false;
    char quoteChar = '\0';
    
    for (int i = 0; i < expression.length(); i++) {
        char c = expression.charAt(i);
        
        if (!inQuote && (c == '\'' || c == '"')) {
            // 开始引用状态
            inQuote = true;
            quoteChar = c;
            currentToken.append(c);
        } else if (inQuote && c == quoteChar) {
            // 结束引用状态
            inQuote = false;
            currentToken.append(c);
            tokens.add(new Token(TokenType.KEYWORD, currentToken.toString()));
            currentToken.setLength(0);
        } else if (!inQuote && c == '|' && i + 1 < expression.length() && expression.charAt(i + 1) == '|') {
            // OR运算符
            if (currentToken.length() > 0) {
                tokens.add(new Token(TokenType.KEYWORD, currentToken.toString().trim()));
                currentToken.setLength(0);
            }
            tokens.add(new Token(TokenType.OR, "||"));
            i++; // 跳过第二个|
        } else if (!inQuote && c == '&' && i + 1 < expression.length() && expression.charAt(i + 1) == '&') {
            // AND运算符
            if (currentToken.length() > 0) {
                tokens.add(new Token(TokenType.KEYWORD, currentToken.toString().trim()));
                currentToken.setLength(0);
            }
            tokens.add(new Token(TokenType.AND, "&&"));
            i++; // 跳过第二个&
        } else if (!inQuote && c == '(') {
            // 左括号
            tokens.add(new Token(TokenType.LEFT_PAREN, "("));
        } else if (!inQuote && c == ')') {
            // 右括号
            if (currentToken.length() > 0) {
                tokens.add(new Token(TokenType.KEYWORD, currentToken.toString().trim()));
                currentToken.setLength(0);
            }
            tokens.add(new Token(TokenType.RIGHT_PAREN, ")"));
        } else {
            currentToken.append(c);
        }
    }
    
    return tokens;
}
```

#### 语法分析器 (Parser) - 递归下降算法
```java
private ExpressionNode parseExpression(List<Token> tokens) {
    return parseOrExpression(tokens);
}

private ExpressionNode parseOrExpression(List<Token> tokens) {
    ExpressionNode left = parseAndExpression(tokens);
    
    while (currentToken().getType() == TokenType.OR) {
        consume(TokenType.OR);
        ExpressionNode right = parseAndExpression(tokens);
        left = new BinaryOpNode(left, "OR", right);
    }
    
    return left;
}

private ExpressionNode parseAndExpression(List<Token> tokens) {
    ExpressionNode left = parsePrimaryExpression(tokens);
    
    while (currentToken().getType() == TokenType.AND) {
        consume(TokenType.AND);
        ExpressionNode right = parsePrimaryExpression(tokens);
        left = new BinaryOpNode(left, "AND", right);
    }
    
    return left;
}

private ExpressionNode parsePrimaryExpression(List<Token> tokens) {
    if (currentToken().getType() == TokenType.LEFT_PAREN) {
        consume(TokenType.LEFT_PAREN);
        ExpressionNode expr = parseExpression(tokens);
        consume(TokenType.RIGHT_PAREN);
        return new ParenthesesNode(expr);
    } else if (currentToken().getType() == TokenType.KEYWORD) {
        String keyword = currentToken().getValue();
        consume(TokenType.KEYWORD);
        return new KeywordNode(keyword);
    } else {
        throw new IllegalArgumentException("意外的token: " + currentToken());
    }
}
```

#### SQL生成器 (Code Generator)
```java
private String generateSQL(ExpressionNode node) {
    if (node instanceof KeywordNode) {
        KeywordNode keywordNode = (KeywordNode) node;
        String cleanKeyword = removeQuotes(keywordNode.getKeyword());
        return searchMethod.buildSingleCondition(fieldName, cleanKeyword);
    } else if (node instanceof BinaryOpNode) {
        BinaryOpNode binaryNode = (BinaryOpNode) node;
        String leftSQL = generateSQL(binaryNode.getLeft());
        String rightSQL = generateSQL(binaryNode.getRight());
        String operator = binaryNode.getOperator();
        return leftSQL + " " + operator + " " + rightSQL;
    } else if (node instanceof ParenthesesNode) {
        ParenthesesNode parenNode = (ParenthesesNode) node;
        String innerSQL = generateSQL(parenNode.getInner());
        return "( " + innerSQL + " )";
    } else {
        throw new IllegalArgumentException("未知的表达式节点类型: " + node.getClass());
    }
}
```

---

*本文档基于当前系统架构编写，随系统演进持续更新维护。* 

## 六、VariantFieldConverter的深度解析

### 6.1 VariantFieldConverter的核心作用和设计背景

#### 设计背景与技术需求
**Doris Variant类型特性**：
- Doris数据库的Variant类型支持JSON数据存储和查询
- 查询嵌套字段时必须使用括号语法：`message['logId']`
- 直接使用点语法会导致SQL语法错误

**用户体验需求**：
- 用户希望使用更直观的点语法：`message.logId`
- 前端界面需要支持字段自动补全和提示
- 减少用户学习成本，提高查询效率

**系统转换需求**：
- 自动将用户输入的点语法转换为Doris要求的括号语法
- 支持多层嵌套字段转换
- 保证转换的准确性和安全性

#### VariantFieldConverter的三大核心功能
```java
@Component
public class VariantFieldConverter {
    
    // 1. WHERE条件转换
    public String convertWhereClause(String whereClause);
    
    // 2. SELECT字段转换 (带别名)
    public List<String> convertSelectFields(List<String> fields);
    
    // 3. TOPN字段转换 (仅转换，不加别名)
    public String convertTopnField(String field);
}
```

### 6.2 用户输入支持的完整范围

#### 支持的输入格式分类

**1. 简单嵌套字段**：
```java
// 两层嵌套
"message.logId"           → "message['logId']"
"request.method"          → "request['method']"
"response.status_code"    → "response['status_code']"
"user.profile"            → "user['profile']"

// 业务常见字段
"order.payment_method"    → "order['payment_method']"
"error.error_code"        → "error['error_code']"
"trace.span_id"           → "trace['span_id']"
```

**2. 多层嵌套字段**：
```java
// 三层嵌套
"user.profile.name"                    → "user['profile']['name']"
"business.order.payment_method"        → "business['order']['payment_method']"
"request.headers.content_type"         → "request['headers']['content_type']"

// 四层嵌套
"trace.spans.operations.duration"      → "trace['spans']['operations']['duration']"
"error.details.exception.stack_trace"  → "error['details']['exception']['stack_trace']"
"system.network.interface.statistics"  → "system['network']['interface']['statistics']"

// 五层及以上嵌套
"business.order.shipping.address.city"           → "business['order']['shipping']['address']['city']"
"monitoring.metrics.system.cpu.usage.percentage" → "monitoring['metrics']['system']['cpu']['usage']['percentage']"
```

**3. 复杂业务场景字段**：
```java
// 微服务架构字段
"service_info.name"                    → "service_info['name']"
"service_info.version"                 → "service_info['version']" 
"service_info.instance_id"             → "service_info['instance_id']"

// 请求响应字段
"request.headers.authorization"        → "request['headers']['authorization']"
"response.body.data.user.profile"      → "response['body']['data']['user']['profile']"
"request.query_params.page_size"       → "request['query_params']['page_size']"

// 金融交易字段
"transaction.amount.currency"          → "transaction['amount']['currency']"
"account.balance.available_amount"     → "account['balance']['available_amount']"
"risk_assessment.score.final_score"    → "risk_assessment['score']['final_score']"

// 物联网设备字段
"device.sensors.temperature.value"     → "device['sensors']['temperature']['value']"
"device.location.coordinates.latitude" → "device['location']['coordinates']['latitude']"
"device.status.connectivity.wifi"      → "device['status']['connectivity']['wifi']"
```

#### 字段名验证规则
```java
private boolean isValidIdentifier(String identifier) {
    if (identifier == null || identifier.isEmpty()) {
        return false;
    }

    // 1. 第一个字符检查：字母、下划线或Unicode字母
    char firstChar = identifier.charAt(0);
    if (!Character.isLetter(firstChar) && firstChar != '_') {
        return false;
    }

    // 2. 后续字符检查：字母、数字、下划线或Unicode字符
    for (int i = 1; i < identifier.length(); i++) {
        char c = identifier.charAt(i);
        if (!Character.isLetterOrDigit(c) && c != '_' && !isValidUnicodeChar(c)) {
            return false;
        }
    }

    return true;
}

// 支持Unicode字符（中文、日文、韩文等）
private boolean isValidUnicodeChar(char c) {
    return Character.getType(c) == Character.OTHER_LETTER ||
           Character.getType(c) == Character.LETTER_NUMBER;
}
```

#### 不支持转换的情况
```java
// 已经是括号语法的字段
"message['level']"               // 不转换
"data['user']['name']"           // 不转换

// 包含引号的字段  
"message'field"                  // 不转换
"data\"field"                    // 不转换

// 不符合标识符规范的字段
"123field.subfield"              // 不转换（以数字开头）
"field-.subfield"                // 不转换（包含非法字符）
"field..subfield"                // 不转换（连续点号）

// 值中包含点号的情况会被正确处理
"field.subfield = 'value.with.dots'"  // 只转换字段名，不转换值
```

### 6.3 转换后生成的SQL语句详细结构

#### 1. SELECT字段转换（带别名）
```sql
-- 输入字段列表
["log_time", "service_name", "request.method", "response.status_code", "error.details.message"]

-- 转换后的SELECT语句
SELECT log_time,
       service_name,
       request['method'] AS 'request.method',
       response['status_code'] AS 'response.status_code',
       error['details']['message'] AS 'error.details.message'
FROM table_name
```

**SELECT字段转换逻辑**：
```java
public List<String> convertSelectFields(List<String> fields) {
    List<String> convertedFields = new ArrayList<>();
    for (String field : fields) {
        String trimmedField = field.trim();
        
        if (isDotSyntax(trimmedField)) {
            // 转换点语法并添加别名
            String bracketSyntax = convertDotToBracketSyntax(trimmedField);
            String fieldWithAlias = bracketSyntax + " AS '" + trimmedField + "'";
            convertedFields.add(fieldWithAlias);
        } else {
            // 普通字段保持不变
            convertedFields.add(field);
        }
    }
    return convertedFields;
}
```

#### 2. WHERE条件转换（直接替换）
```sql
-- 输入WHERE条件
"order_info.status = 'paid' AND user_info.level = 'vip' AND product_info.category.main = 'electronics'"

-- 转换后的WHERE条件
"order_info['status'] = 'paid' AND user_info['level'] = 'vip' AND product_info['category']['main'] = 'electronics'"

-- 完整SQL示例
SELECT * FROM table_name
WHERE log_time >= '2023-01-01 00:00:00' AND log_time < '2023-01-02 00:00:00'
  AND order_info['status'] = 'paid' 
  AND user_info['level'] = 'vip' 
  AND product_info['category']['main'] = 'electronics'
```

**WHERE条件转换的安全机制**：
```java
public String convertWhereClause(String whereClause) {
    if (whereClause == null || whereClause.trim().isEmpty()) {
        return whereClause;
    }
    
    // 使用安全转换，保护引号内的内容
    return convertDotSyntaxSafely(whereClause);
}

private String convertDotSyntaxSafely(String input) {
    StringBuilder result = new StringBuilder();
    StringBuilder currentToken = new StringBuilder();
    boolean inQuote = false;
    char quoteChar = '\0';
    
    for (char c : input.toCharArray()) {
        if (!inQuote && (c == '\'' || c == '"')) {
            // 进入引号保护状态
            flushToken(result, currentToken, false);
            inQuote = true;
            quoteChar = c;
            result.append(c);
        } else if (inQuote && c == quoteChar) {
            // 退出引号保护状态
            inQuote = false;
            result.append(c);
        } else if (inQuote) {
            // 引号内的内容不转换
            result.append(c);
        } else {
            // 引号外的内容需要检查是否转换
            currentToken.append(c);
            if (Character.isWhitespace(c) || c == '(' || c == ')' || c == '=' || c == '<' || c == '>') {
                flushToken(result, currentToken, false);
            }
        }
    }
    
    flushToken(result, currentToken, false);
    return result.toString();
}
```

#### 3. TOPN字段转换（仅转换）
```sql
-- 输入TOPN字段
["business.region", "user.department", "service.instance"]

-- 转换后的TOPN查询
SELECT TOPN(business['region'], 5) AS 'business.region',
       TOPN(user['department'], 5) AS 'user.department',
       TOPN(service['instance'], 5) AS 'service.instance'
FROM (
    SELECT * FROM table_name
    WHERE log_time >= '2023-01-01 00:00:00' AND log_time < '2023-01-02 00:00:00'
    ORDER BY log_time DESC
    LIMIT 5000 OFFSET 0
) AS sample_data
```

**TOPN字段转换逻辑**：
```java
public String convertTopnField(String field) {
    if (field == null || field.trim().isEmpty()) {
        return field;
    }

    String trimmedField = field.trim();
    if (isDotSyntax(trimmedField)) {
        // 仅转换，不添加别名（TOPN函数需要纯字段名）
        return convertDotToBracketSyntax(trimmedField);
    }

    return field;
}
```

### 6.4 核心转换算法实现

#### 点语法到括号语法的转换核心逻辑
```java
private String convertDotToBracketSyntax(String dotSyntax) {
    String[] parts = dotSyntax.split("\\.");
    StringBuilder result = new StringBuilder();
    
    // 第一部分：根字段名
    result.append(parts[0]);
    
    // 后续部分：转换为括号语法
    for (int i = 1; i < parts.length; i++) {
        result.append("['").append(parts[i]).append("']");
    }
    
    return result.toString();
}
```

#### 转换示例详解
```java
// 示例1：简单嵌套
输入: "message.logId"
分割: ["message", "logId"]
处理: message + ['logId']
输出: "message['logId']"

// 示例2：三层嵌套
输入: "user.profile.name"
分割: ["user", "profile", "name"]
处理: user + ['profile'] + ['name']
输出: "user['profile']['name']"

// 示例3：复杂业务嵌套
输入: "transaction.payment.credit_card.last_four_digits"
分割: ["transaction", "payment", "credit_card", "last_four_digits"]
处理: transaction + ['payment'] + ['credit_card'] + ['last_four_digits']
输出: "transaction['payment']['credit_card']['last_four_digits']"

// 示例4：深层嵌套
输入: "monitoring.metrics.system.cpu.usage.percentage"
分割: ["monitoring", "metrics", "system", "cpu", "usage", "percentage"] 
处理: monitoring + ['metrics'] + ['system'] + ['cpu'] + ['usage'] + ['percentage']
输出: "monitoring['metrics']['system']['cpu']['usage']['percentage']"
```

### 6.5 安全特性和性能优化

#### 引号保护机制详解
```java
// 复杂WHERE条件的安全转换示例
输入: "message.level = 'ERROR' AND message.content LIKE '%user.action.login%' AND timestamp > '2023-01-01'"

处理过程:
1. message.level = 'ERROR'        → message['level'] = 'ERROR'        (转换字段名)
2. message.content LIKE '%user.action.login%' → message['content'] LIKE '%user.action.login%' (保护引号内容)  
3. timestamp > '2023-01-01'       → timestamp > '2023-01-01'          (普通字段+保护引号)

输出: "message['level'] = 'ERROR' AND message['content'] LIKE '%user.action.login%' AND timestamp > '2023-01-01'"
```

#### 性能优化机制
```java
// 1. 预检查避免不必要的转换
private boolean needsVariantConversion(String text) {
    // 快速检查：不包含点号则不需要转换
    if (!text.contains(".")) return false;
    
    // 快速检查：已包含括号语法则不需要转换
    if (text.contains("[") && text.contains("'")) return false;
    
    // 只有可能需要转换时才进行详细检查
    return true;
}

// 2. 字段列表转换优化
public List<String> convertSelectFields(List<String> fields) {
    // 检查是否需要转换
    boolean needsConversion = fields.stream().anyMatch(this::needsVariantConversion);
    
    if (!needsConversion) {
        return fields; // 返回原始对象，避免不必要的复制
    }
    
    // 只有需要转换时才执行转换逻辑
    return variantFieldConverter.convertSelectFields(fields);
}

// 3. WHERE条件列表转换优化
public List<String> convertWhereClauses(List<String> whereClauses) {
    // 检查是否需要转换
    boolean needsConversion = whereClauses.stream().anyMatch(this::needsVariantConversion);
    
    if (!needsConversion) {
        return whereClauses; // 返回原始对象，避免不必要的复制
    }
    
    // 执行转换
    return whereClauses.stream()
            .map(variantFieldConverter::convertWhereClause)
            .collect(Collectors.toList());
}
```

### 6.6 实际业务场景应用示例

#### 电商系统日志查询场景
```java
// 用户输入的查询条件
SELECT log_time, service_name, 
       order.order_id, order.status, order.payment.method,
       user.user_id, user.profile.level,
       product.category.main, product.price.final_amount
FROM order_logs
WHERE order.status = 'completed' 
  AND user.profile.level IN ('vip', 'premium')
  AND product.category.main = 'electronics'
  AND order.payment.method = 'credit_card'

// 系统自动转换后的SQL
SELECT log_time, service_name,
       order['order_id'] AS 'order.order_id',
       order['status'] AS 'order.status', 
       order['payment']['method'] AS 'order.payment.method',
       user['user_id'] AS 'user.user_id',
       user['profile']['level'] AS 'user.profile.level',
       product['category']['main'] AS 'product.category.main',
       product['price']['final_amount'] AS 'product.price.final_amount'
FROM order_logs  
WHERE log_time >= '2023-01-01 00:00:00' AND log_time < '2023-01-02 00:00:00'
  AND order['status'] = 'completed'
  AND user['profile']['level'] IN ('vip', 'premium') 
  AND product['category']['main'] = 'electronics'
  AND order['payment']['method'] = 'credit_card'
```

#### 金融交易监控场景
```java
// TOPN字段分布查询
输入字段: ["transaction.type", "account.region", "risk.level"]

转换后的查询:
SELECT TOPN(transaction['type'], 5) AS 'transaction.type',
       TOPN(account['region'], 5) AS 'account.region', 
       TOPN(risk['level'], 5) AS 'risk.level'
FROM (
    SELECT * FROM financial_logs
    WHERE log_time >= '2023-01-01 00:00:00' AND log_time < '2023-01-02 00:00:00'
      AND transaction['amount']['value'] > 10000
      AND risk['score']['final'] > 0.8
    ORDER BY log_time DESC
    LIMIT 5000 OFFSET 0
) AS sample_data
```

#### 微服务链路追踪场景
```java
// 复杂嵌套字段查询
WHERE trace.trace_id = 'trace-12345'
  AND span.operation.name = 'payment-processing'
  AND span.duration.milliseconds > 1000
  AND service.instance.region = 'us-west-1'
  AND error.details.exception.type = 'TimeoutException'

// 转换后
WHERE trace['trace_id'] = 'trace-12345'
  AND span['operation']['name'] = 'payment-processing'  
  AND span['duration']['milliseconds'] > 1000
  AND service['instance']['region'] = 'us-west-1'
  AND error['details']['exception']['type'] = 'TimeoutException'
```

---

## 总结

秒查日志查询系统体现了极其精细和完善的企业级架构设计：

### 核心特色
1. **分层架构清晰**：从验证层到执行器层，职责明确，易于维护和扩展
2. **配置驱动设计**：支持不同模块使用不同搜索策略，灵活性极强
3. **性能优化完善**：并行查询、采样优化、桶数量智能控制
4. **用户体验优秀**：点语法自动转换、预定义时间范围、智能时间颗粒度
5. **扩展性强大**：支持多种数据库、多种搜索方法、可插拔组件设计

### 技术亮点
- **Kibana算法时间颗粒度计算**：22种标准间隔，毫秒级精度支持
- **配置驱动的搜索方法**：LIKE/MATCH_PHRASE/MATCH_ANY/MATCH_ALL四种方法灵活配置
- **复杂表达式解析引擎**：支持AND/OR/括号嵌套的完整语法解析
- **Variant字段智能转换**：点语法到括号语法的安全转换，支持深层嵌套
- **两层查询优化策略**：采样+TOPN的高性能字段分布查询
- **异步并行查询**：CompletableFuture实现的查询性能优化

### 架构价值
这个系统不仅是技术实现的典范，更是在用户体验和系统性能之间找到完美平衡的企业级解决方案。它展示了如何通过精心的架构设计，将复杂的技术细节隐藏在简洁的用户界面背后，同时保持高性能和高可扩展性。 