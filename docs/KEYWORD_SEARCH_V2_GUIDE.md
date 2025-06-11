# 关键字搜索 v2.0 使用指南

## 概述

关键字搜索 v2.0 采用基于 `MATCH_PHRASE` 的简化实现，提供更直观、更容易理解的搜索体验。

## 核心特性

### ✅ v2.0 的优势

- **简单直观**: 每个关键字都是独立的 `MATCH_PHRASE` 条件
- **逻辑清晰**: 表达式结构与SQL输出一一对应
- **易于调试**: 生成的SQL条件容易理解和排查
- **统一处理**: 所有关键字都使用相同的 `MATCH_PHRASE` 函数

### 🚀 快速开始

```java
// 1. 引入字段常量
import com.hinadt.miaocha.common.constants.FieldConstants;

// 2. 确保表包含 message 字段
// 系统会自动验证表结构中是否存在 message 字段

// 3. 使用关键字搜索
LogSearchDTO searchDto = new LogSearchDTO();
searchDto.setKeywords(Arrays.asList("error", "timeout"));
```

## 语法规则

### 基础语法

| 表达式类型 |         输入示例         |                               输出SQL                                |    说明     |
|-------|----------------------|--------------------------------------------------------------------|-----------|
| 单个关键字 | `error`              | `message MATCH_PHRASE 'error'`                                     | 最简单的搜索    |
| 短语搜索  | `'database error'`   | `message MATCH_PHRASE 'database error'`                            | 完整短语匹配    |
| AND运算 | `error && critical`  | `message MATCH_PHRASE 'error' AND message MATCH_PHRASE 'critical'` | 两个条件都必须满足 |
| OR运算  | `error \|\| warning` | `message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning'`   | 任一条件满足即可  |

### 复杂表达式

```javascript
// 基本组合
'error' && 'critical'
// ↓ 输出
message MATCH_PHRASE 'error' AND message MATCH_PHRASE 'critical'

// 括号优先级
('error' || 'warning') && 'critical'
// ↓ 输出  
( message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning' ) AND message MATCH_PHRASE 'critical'

// 复杂嵌套
('user' || 'order') && ('service' || 'api')
// ↓ 输出
( message MATCH_PHRASE 'user' OR message MATCH_PHRASE 'order' ) AND ( message MATCH_PHRASE 'service' OR message MATCH_PHRASE 'api' )
```

## 实际使用示例

### 1. 日志级别搜索

```java
// 搜索错误或警告日志
keywords: ["error || warning"]
// SQL: message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning'

// 搜索严重错误
keywords: ["error && critical"]  
// SQL: message MATCH_PHRASE 'error' AND message MATCH_PHRASE 'critical'
```

### 2. 服务监控

```java
// 搜索特定服务的超时问题
keywords: ["('user-service' || 'order-service') && 'timeout'"]
// SQL: ( message MATCH_PHRASE 'user-service' OR message MATCH_PHRASE 'order-service' ) AND message MATCH_PHRASE 'timeout'

// 搜索数据库相关问题
keywords: ["'database' && ('connection' || 'timeout' || 'deadlock')"]
// SQL: message MATCH_PHRASE 'database' AND ( message MATCH_PHRASE 'connection' OR message MATCH_PHRASE 'timeout' OR message MATCH_PHRASE 'deadlock' )
```

### 3. 业务场景

```java
// 用户操作异常
keywords: ["'user' && ('login' || 'register') && 'failed'"]
// SQL: message MATCH_PHRASE 'user' AND ( message MATCH_PHRASE 'login' OR message MATCH_PHRASE 'register' ) AND message MATCH_PHRASE 'failed'

// 支付流程监控
keywords: ["'payment' && ('success' || 'failed' || 'timeout')"]
// SQL: message MATCH_PHRASE 'payment' AND ( message MATCH_PHRASE 'success' OR message MATCH_PHRASE 'failed' OR message MATCH_PHRASE 'timeout' )
```

## 与 v1.0 的差异

|    特性     |             v1.0 (已废弃)              |                           v2.0 (当前版本)                            |          说明          |
|-----------|-------------------------------------|------------------------------------------------------------------|----------------------|
| **核心函数**  | `MATCH_ANY` / `MATCH_ALL`           | `MATCH_PHRASE`                                                   | v2.0统一使用MATCH_PHRASE |
| **关键字合并** | 智能合并相同类型的关键字                        | 每个关键字独立处理                                                        | v2.0更直观              |
| **表达式优化** | 复杂的优化逻辑                             | 保持原始表达式结构                                                        | v2.0易于理解             |
| **生成SQL** | `message MATCH_ANY 'error warning'` | `message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning'` | v2.0结构更清晰            |

### 迁移对比示例

```java
// 表达式: "error || warning"

// v1.0 输出:
message MATCH_ANY 'error warning'

// v2.0 输出:  
message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning'

// 表达式: "('error' || 'warning') && 'critical'"

// v1.0 输出:
message MATCH_ANY 'error warning' AND message MATCH_ANY 'critical'

// v2.0 输出:
( message MATCH_PHRASE 'error' OR message MATCH_PHRASE 'warning' ) AND message MATCH_PHRASE 'critical'
```

## 最佳实践

### ✅ 推荐做法

1. **使用引号**: 对于包含空格或特殊字符的关键字使用引号

   ```java
   keywords: ["'user service error'"]  // ✅ 正确
   keywords: ["user service error"]    // ❌ 可能解析错误
   ```
2. **合理使用括号**: 明确表达逻辑优先级

   ```java
   keywords: ["('error' || 'warning') && 'critical'"]  // ✅ 清晰的逻辑
   keywords: ["error || warning && critical"]          // ❌ 优先级不明确
   ```
3. **中文关键字**: 中文关键字必须使用引号

   ```java
   keywords: ["'用户服务异常'"]     // ✅ 正确
   keywords: ["用户服务异常"]       // ❌ 解析失败
   ```

### ⚠️ 注意事项

1. **message字段必需**: 表必须包含 `message` 字段，系统会自动验证
2. **引号配对**: 确保引号正确配对，避免语法错误
3. **嵌套深度**: 虽然支持深层嵌套，但建议保持适度复杂度

## 错误处理

### 表结构验证

```java
// 系统会自动检查表结构
try {
    tableValidationService.validateTableStructure(datasourceId, tableName);
} catch (BusinessException e) {
    if (e.getErrorCode() == ErrorCode.TABLE_MESSAGE_FIELD_MISSING) {
        // 表缺少 message 字段
        log.error("表 {} 缺少必需的 message 字段", tableName);
    }
}
```

### 常见错误

|    错误类型     |    示例    |       解决方案       |
|-------------|----------|------------------|
| 引号不匹配       | `'error` | 确保引号成对出现         |
| 括号不匹配       | `(error` | 确保括号成对出现         |
| 空关键字列表      | `[]`     | 提供至少一个有效关键字      |
| message字段缺失 | -        | 在表中添加 message 字段 |

## 测试验证

系统提供全面的单元测试，包括：

- ✅ 语法解析测试 (50+ 测试用例)
- ✅ 边界情况测试 (空值、特殊字符、Unicode)
- ✅ 性能测试 (大量关键字处理)
- ✅ 表结构验证测试
- ✅ 错误处理测试

```java
// 运行测试
mvn test -Dtest=KeywordPhraseConditionBuilderTest
mvn test -Dtest=TableValidationServiceImplTest
```

## 技术细节

### 字段常量

```java
// message 字段已定义为常量
public class FieldConstants {
    public static final String MESSAGE_FIELD = "message";
}
```

### 处理器优先级

```java
@Component
@Order(5)  // 最高优先级
public class KeywordPhraseConditionBuilder implements SearchConditionBuilder {
    // 处理所有关键字搜索请求
}

// 废弃的处理器优先级已降低到 20-25
```

### 性能特性

- **时间复杂度**: O(n)，n为关键字数量
- **空间复杂度**: O(n)
- **处理能力**: 可处理100+关键字的复杂表达式

## 升级日志

### v2.0.0 (当前版本)

- ✅ 引入 `KeywordPhraseConditionBuilder`
- ✅ 添加 `FieldConstants.MESSAGE_FIELD` 常量
- ✅ 新增表结构验证错误码
- ✅ 废弃 v1.0 的三个Builder类
- ✅ 更新文档和测试用例

### v1.0.0 (已废弃)

- ⚠️ 使用 `MATCH_ANY`/`MATCH_ALL` 实现
- ⚠️ 复杂的关键字合并逻辑
- ⚠️ 已标记为 `@Deprecated`

---

> **重要提醒**: v1.0 的实现仍然保留在代码中（标记为废弃），这是为了可能的未来迁移需求。请使用 v2.0 的新实现进行所有新的开发工作。

