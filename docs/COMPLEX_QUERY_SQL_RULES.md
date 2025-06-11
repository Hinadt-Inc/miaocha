# 复杂查询 Doris SQL 产出规则文档

## 概述

本文档详细阐述了日志管理系统中复杂查询表达式转换为 Doris SQL 条件的规则和机制。系统支持关键字搜索、逻辑运算符（AND/OR）、括号嵌套等复杂查询功能。

> **版本更新说明**：  
> **v2.0 (当前版本)**: 引入基于MATCH_PHRASE的简化实现，提供更直观的关键字搜索体验。  
> **v1.0 (已废弃)**: 使用MATCH_ANY/MATCH_ALL的智能优化实现，保留用于可能的未来迁移。

## 系统架构

### 1. 核心组件

**v2.0 (当前版本) - 基于MATCH_PHRASE的实现**

|               组件                |            职责            |  优先级   |
|---------------------------------|--------------------------|--------|
| `KeywordPhraseConditionBuilder` | 处理所有关键字表达式（MATCH_PHRASE） | 5（最高）  |
| `WhereSqlConditionBuilder`      | 处理WHERE条件                | 30（最低） |

**v1.0 (已废弃) - 基于MATCH_ANY/MATCH_ALL的实现**

|                组件                 |        职责         |   优先级   |
|-----------------------------------|-------------------|---------|
| `KeywordComplexExpressionBuilder` | 处理复杂表达式（包含括号、运算符） | 20（已降低） |
| `KeywordMatchAllConditionBuilder` | 处理简单AND表达式        | 25（已降低） |
| `KeywordMatchAnyConditionBuilder` | 处理简单关键字搜索         | 25（已降低） |

### 2. 处理流程

```
用户输入 → SearchConditionManager → 按优先级选择Builder → 生成SQL条件
```

## SQL 产出规则

### 1. v2.0 基础规则表格 (当前版本 - MATCH_PHRASE)

|     输入类型     |                   示例输入                   |             处理组件              |                                                  SQL 输出                                                  |         说明          |
|--------------|------------------------------------------|-------------------------------|----------------------------------------------------------------------------------------------------------|---------------------|
| **单个关键字**    | `error`                                  | KeywordPhraseConditionBuilder | `message MATCH_PHRASE 'error'`                                                                           | 精确短语匹配              |
| **带引号关键字**   | `'timeout'`                              | KeywordPhraseConditionBuilder | `message MATCH_PHRASE 'timeout'`                                                                         | 包含特殊字符的关键字          |
| **多词关键字**    | `'database connection'`                  | KeywordPhraseConditionBuilder | `message MATCH_PHRASE 'database connection'`                                                             | 完整短语搜索              |
| **简单AND表达式** | `error && timeout`                       | KeywordPhraseConditionBuilder | `message MATCH_PHRASE 'error' AND message MATCH_PHRASE 'timeout'`                                        | 每个关键字独立MATCH_PHRASE |
| **简单OR表达式**  | `error \|\| warning`                     | KeywordPhraseConditionBuilder | `message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning'`                                         | 每个关键字独立MATCH_PHRASE |
| **复杂括号表达式**  | `('error' \|\| 'warning') && 'critical'` | KeywordPhraseConditionBuilder | `( message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning' ) AND message MATCH_PHRASE 'critical'` | 保持表达式逻辑结构           |

### 2. v1.0 基础规则表格 (已废弃 - MATCH_ANY/MATCH_ALL)

|     输入类型     |                   示例输入                   |              处理组件               |                                SQL 输出                                |      说明       |
|--------------|------------------------------------------|---------------------------------|----------------------------------------------------------------------|---------------|
| **单个关键字**    | `error`                                  | KeywordMatchAnyConditionBuilder | `message MATCH_ANY 'error'`                                          | 简单关键字搜索       |
| **带引号关键字**   | `'timeout'`                              | KeywordMatchAnyConditionBuilder | `message MATCH_ANY 'timeout'`                                        | 包含特殊字符的关键字    |
| **多词关键字**    | `'database connection'`                  | KeywordMatchAnyConditionBuilder | `message MATCH_ANY 'database connection'`                            | 短语搜索          |
| **简单AND表达式** | `error && timeout`                       | KeywordMatchAllConditionBuilder | `message MATCH_ALL 'error timeout'`                                  | 所有关键字都必须存在    |
| **简单OR表达式**  | `error \|\| warning`                     | KeywordComplexExpressionBuilder | `message MATCH_ANY 'error warning'`                                  | 任一关键字存在即可     |
| **复杂括号表达式**  | `('error' \|\| 'warning') && 'critical'` | KeywordComplexExpressionBuilder | `message MATCH_ANY 'error warning' AND message MATCH_ANY 'critical'` | OR关键字合并，AND连接 |

### 3. v2.0 详细产出规则 (当前版本)

#### 3.1 MATCH_PHRASE规则

|   表达式类型    |                           输入示例                            |        解析过程         |                                                                     SQL输出                                                                      |      说明       |
|------------|-----------------------------------------------------------|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| 单个关键字      | `error`                                                   | 直接转换                | `message MATCH_PHRASE 'error'`                                                                                                                 | 最简单的情况        |
| 带引号短语      | `'database connection error'`                             | 去除引号，保留内容           | `message MATCH_PHRASE 'database connection error'`                                                                                             | 完整短语精确匹配      |
| 双关键字AND    | `error && critical`                                       | 分解为两个独立MATCH_PHRASE | `message MATCH_PHRASE 'error' AND message MATCH_PHRASE 'critical'`                                                                             | 两个条件都必须满足     |
| 双关键字OR     | `error \|\| warning`                                      | 分解为两个独立MATCH_PHRASE | `message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning'`                                                                               | 任一条件满足即可      |
| 三关键字AND    | `error && timeout && critical`                            | 连续AND连接             | `message MATCH_PHRASE 'error' AND message MATCH_PHRASE 'timeout' AND message MATCH_PHRASE 'critical'`                                          | 所有条件都必须满足     |
| 三关键字OR     | `error \|\| warning \|\| info`                            | 连续OR连接              | `message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning' OR message MATCH_PHRASE 'info'`                                                | 任一条件满足即可      |
| 简单括号OR+AND | `('error' \|\| 'warning') && 'critical'`                  | 括号内OR，然后AND外部条件     | `( message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning' ) AND message MATCH_PHRASE 'critical'`                                       | 保持逻辑结构，不做合并优化 |
| 左AND右括号OR  | `'database' && ('timeout' \|\| 'connection')`             | 左侧AND，右侧括号内OR       | `message MATCH_PHRASE 'database' AND ( message MATCH_PHRASE 'timeout' OR message MATCH_PHRASE 'connection' )`                                  | 保持逻辑结构        |
| 双括号AND     | `('user' \|\| 'order') && ('service' \|\| 'api')`         | 两个括号内分别OR，然后AND连接   | `( message MATCH_PHRASE 'user' OR message MATCH_PHRASE 'order' ) AND ( message MATCH_PHRASE 'service' OR message MATCH_PHRASE 'api' )`         | 复杂逻辑的直观表示     |
| 嵌套括号       | `(('error' \|\| 'warning') && 'critical') \|\| 'timeout'` | 递归解析嵌套结构            | `( ( message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning' ) AND message MATCH_PHRASE 'critical' ) OR message MATCH_PHRASE 'timeout'` | 支持深层嵌套        |

#### 3.2 关键字规则

| 关键字格式  |         示例         |       提取结果       |             MATCH_PHRASE输出              |     备注      |
|--------|--------------------|------------------|-----------------------------------------|-------------|
| 无引号单词  | `error`            | `error`          | `message MATCH_PHRASE 'error'`          | 直接使用        |
| 带引号字符串 | `'database error'` | `database error` | `message MATCH_PHRASE 'database error'` | 去除引号，保留内容   |
| 空字符串   | `''`               | `(空)`            | `(空条件)`                                 | 返回空结果       |
| 中文关键字  | `'错误日志'`           | `错误日志`           | `message MATCH_PHRASE '错误日志'`           | 支持Unicode字符 |

#### 3.3 v2.0与v1.0对比

|                  表达式示例                   |                                          v2.0 输出 (MATCH_PHRASE)                                          |                       v1.0 输出 (MATCH_ANY/ALL)                        |         差异说明          |
|------------------------------------------|----------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------|-----------------------|
| `error`                                  | `message MATCH_PHRASE 'error'`                                                                           | `message MATCH_ANY 'error'`                                          | 函数不同，但功能类似            |
| `error && timeout`                       | `message MATCH_PHRASE 'error' AND message MATCH_PHRASE 'timeout'`                                        | `message MATCH_ALL 'error timeout'`                                  | v2.0每个关键字独立，v1.0合并关键字 |
| `error \|\| warning`                     | `message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning'`                                         | `message MATCH_ANY 'error warning'`                                  | v2.0每个关键字独立，v1.0合并关键字 |
| `('error' \|\| 'warning') && 'critical'` | `( message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning' ) AND message MATCH_PHRASE 'critical'` | `message MATCH_ANY 'error warning' AND message MATCH_ANY 'critical'` | v2.0保持结构，v1.0优化合并     |

### 4. v1.0 详细产出规则 (已废弃)

#### 4.1 关键字规则

| 关键字格式  |         示例         |       提取结果       |     备注      |
|--------|--------------------|------------------|-------------|
| 无引号单词  | `error`            | `error`          | 直接使用        |
| 带引号字符串 | `'database error'` | `database error` | 去除引号，保留内容   |
| 空字符串   | `''`               | `(空)`            | 返回空结果       |
| 中文关键字  | `'错误日志'`           | `错误日志`           | 支持Unicode字符 |

#### 2.2 运算符优先级

|  优先级  |    运算符    |   说明    |           示例            |
|-------|-----------|---------|-------------------------|
| 1（最高） | `()` 括号   | 改变运算优先级 | `('a' \|\| 'b') && 'c'` |
| 2     | `&&` AND  | 逻辑与运算   | `'a' && 'b'`            |
| 3（最低） | `\|\|` OR | 逻辑或运算   | `'a' \|\| 'b'`          |

#### 2.3 Doris SQL 函数映射

|  逻辑操作   |       Doris 函数       |         参数格式          |       用途       |
|---------|----------------------|-----------------------|----------------|
| 单关键字搜索  | `MATCH_ANY`          | `'keyword'`           | 检查消息中是否包含关键字   |
| 多关键字AND | `MATCH_ALL`          | `'keyword1 keyword2'` | 检查消息中是否包含所有关键字 |
| 多关键字OR  | 多个`MATCH_ANY`用`OR`连接 | 各自独立条件                | 检查消息中是否包含任一关键字 |

## 复杂表达式解析示例

### 1. 递归解析流程

| 步骤 |                     输入                     |      处理动作      |                                 中间结果                                 |
|----|--------------------------------------------|----------------|----------------------------------------------------------------------|
| 1  | `('error' \|\| 'warning') && 'critical'`   | 识别为复杂表达式       | 进入KeywordComplexExpressionBuilder                                    |
| 2  | `('error' \|\| 'warning') && 'critical'`   | 规范化空格          | `( 'error' \|\| 'warning' ) && 'critical'`                           |
| 3  | `( 'error' \|\| 'warning' ) && 'critical'` | 解析括号内容         | 括号内:`'error' \|\| 'warning'`                                         |
| 4  | `'error' \|\| 'warning'`                   | 递归解析OR表达式      | `message MATCH_ANY 'error warning'`                                  |
| 5  | 替换括号内容后                                    | 查找顶层AND运算符     | 左:`(message MATCH_ANY 'error warning')` 右:`'critical'`               |
| 6  | 解析右侧表达式                                    | 处理单个关键字        | `message MATCH_ANY 'critical'`                                       |
| 7  | 合并AND表达式                                   | 保持独立的MATCH_ANY | 左右两边都是独立的MATCH_ANY                                                   |
| 8  | 生成最终SQL                                    | 用AND连接         | `message MATCH_ANY 'error warning' AND message MATCH_ANY 'critical'` |

### 2. 复杂示例对照表

|                        表达式                        | 解析步骤  |                                   最终SQL                                   |           业务含义           |
|---------------------------------------------------|-------|---------------------------------------------------------------------------|--------------------------|
| `'user service'`                                  | 单关键字  | `message MATCH_ANY 'user service'`                                        | 包含"user service"短语       |
| `error && timeout`                                | 简单AND | `message MATCH_ALL 'error timeout'`                                       | 同时包含error和timeout        |
| `error \|\| warning \|\| info`                    | 多项OR  | `message MATCH_ANY 'error warning info'`                                  | 包含任一关键字                  |
| `('error' \|\| 'warning') && 'critical'`          | 复杂表达式 | `message MATCH_ANY 'error warning' AND message MATCH_ANY 'critical'`      | OR关键字合并，AND连接            |
| `'database' && ('timeout' \|\| 'connection')`     | 复杂表达式 | `message MATCH_ANY 'database' AND message MATCH_ANY 'timeout connection'` | OR关键字合并，AND连接            |
| `('user' \|\| 'order') && ('service' \|\| 'api')` | 复杂表达式 | `message MATCH_ANY 'user order' AND message MATCH_ANY 'service api'`      | 每个OR都合并关键字，AND连接         |
| `'user' && 'service' && 'critical'`               | 多重AND | `message MATCH_ANY 'user' AND message MATCH_ALL 'service critical'`       | 部分优化：最后两个关键字合并为MATCH_ALL |
| `(('error' \|\| 'warn') && 'critical')`           | 嵌套表达式 | `( (message MATCH_ANY 'error warn') && 'critical' )`                      | 保持部分原始结构，OR关键字合并         |

## 语法验证规则

### 1. 支持的语法

|  语法元素  |         格式          |            示例             |    说明    |
|--------|---------------------|---------------------------|----------|
| 关键字    | `word` 或 `'phrase'` | `error`, `'user service'` | 单词或带引号短语 |
| AND运算符 | `&&`                | `a && b`                  | 逻辑与      |
| OR运算符  | `\|\|`              | `a \|\| b`                | 逻辑或      |
| 括号     | `()`                | `(a \|\| b)`              | 改变优先级    |
| 嵌套     | 最多2层                | `((a \|\| b))`            | 限制嵌套深度   |

### 2. 语法限制

| 限制类型  |    规则    |     错误示例      |    错误信息     |
|-------|----------|---------------|-------------|
| 引号匹配  | 必须成对出现   | `'error`      | "引号不匹配"     |
| 括号匹配  | 必须成对出现   | `(error`      | "括号不匹配"     |
| 运算符位置 | 不能在开头或结尾 | `&& error`    | "运算符使用不正确"  |
| 连续运算符 | 不能连续出现   | `a && \|\| b` | "不能有连续的运算符" |
| 空括号   | 不允许空内容   | `()`          | "空括号"       |
| 嵌套深度  | 最多2层     | `(((a)))`     | "嵌套层级过深"    |

## 边界情况处理

### 1. 特殊输入处理

|  输入情况   |  处理方式   | 输出结果 |   说明    |
|---------|---------|------|---------|
| `null`  | 返回空字符串  | `""` | 空查询条件   |
| `""`    | 返回空字符串  | `""` | 空查询条件   |
| `"   "` | 去除空格后处理 | `""` | 仅空格的输入  |
| `''`    | 提取空关键字  | `""` | 空字符串关键字 |

### 2. 空格处理

| 空格情况  |      示例输入       |      规范化结果      |   说明   |
|-------|-----------------|-----------------|--------|
| 关键字前后 | `  error  `     | `error`         | 自动去除   |
| 运算符周围 | `a&&b`          | `a && b`        | 自动添加空格 |
| 括号周围  | `(a)`           | `( a )`         | 自动添加空格 |
| 引号内空格 | `'  content  '` | `'  content  '` | 保留不变   |

## 性能考虑

### 1. 优化策略

|    优化点    |          策略           |   效果   |
|-----------|-----------------------|--------|
| Builder选择 | 按优先级匹配，避免不必要的复杂解析     | 提高响应速度 |
| 正则表达式     | 预编译常用模式               | 减少编译开销 |
| 字符串操作     | 使用StringBuilder处理大量拼接 | 减少内存分配 |
| 递归深度      | 限制最大嵌套层级              | 防止栈溢出  |

### 2. 复杂度分析

|  表达式类型   | 时间复杂度  | 空间复杂度  |     备注      |
|----------|--------|--------|-------------|
| 简单关键字    | O(1)   | O(1)   | 直接处理        |
| 简单AND/OR | O(n)   | O(n)   | n为关键字数量     |
| 复杂表达式    | O(n*m) | O(n*m) | n为层级，m为关键字数 |

## 测试覆盖

系统包含全面的测试套件：

- **KeywordExpressionParserTest**: 32个测试用例，覆盖语法解析
- **KeywordMatchAnyConditionBuilderTest**: 16个测试用例，覆盖简单搜索
- **SearchConditionBuilderIntegrationTest**: 21个测试用例，覆盖端到端集成

总计69个测试用例，确保系统的稳定性和正确性。

## 系统已知限制

### 1. 复杂表达式简化处理

当前实现对复杂表达式采用简化逻辑，确保基本功能可用：

|   限制类型    |            具体表现            |                   示例                    |                               当前处理结果                                |
|-----------|----------------------------|-----------------------------------------|---------------------------------------------------------------------|
| 多层AND部分优化 | 多个AND关键字的最后两个会合并为MATCH_ALL | `'user' && 'service' && 'critical'`     | `message MATCH_ANY 'user' AND message MATCH_ALL 'service critical'` |
| 嵌套表达式混合处理 | 深层嵌套保持部分原始结构，产生混合格式        | `(('error' \|\| 'warn') && 'critical')` | `( (message MATCH_ANY 'error warn') && 'critical' )`                |

### 2. 工作正常的功能

以下功能完全按预期工作：

|   功能类型   |          示例          |                输出结果                 | 状态 |
|----------|----------------------|-------------------------------------|----|
| 单关键字搜索   | `error`              | `message MATCH_ANY 'error'`         | ✓  |
| 简单AND表达式 | `error && timeout`   | `message MATCH_ALL 'error timeout'` | ✓  |
| 简单OR表达式  | `error \|\| warning` | `message MATCH_ANY 'error warning'` | ✓  |
| 多项OR表达式  | `a \|\| b \|\| c`    | `message MATCH_ANY 'a b c'`         | ✓  |

## 使用建议

### 1. 最佳实践

1. **简单查询优先**: 对于简单关键字，直接使用不带引号的格式
2. **合理使用括号**: 复杂逻辑时明确使用括号表达优先级，但需理解当前简化处理的限制
3. **避免过深嵌套**: 控制在2层以内，提高可读性和性能
4. **关键字规范化**: 包含空格的短语使用引号包围
5. **复杂查询评估**: 使用复杂表达式前，先了解当前实现的简化逻辑

### 2. 常见错误避免

1. **引号不匹配**: 确保每个开引号都有对应的闭引号
2. **运算符位置**: 避免在表达式开头或结尾使用运算符
3. **空括号**: 确保括号内有有效内容
4. **连续运算符**: 避免`&&||`这样的连续运算符
5. **过度依赖复杂逻辑**: 当前实现会简化复杂表达式，请验证实际输出是否符合预期

---

*文档版本: v1.0*  
*最后更新: 2024年6月*  
*维护者: 系统开发团队*
