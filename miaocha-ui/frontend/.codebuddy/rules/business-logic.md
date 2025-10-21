# 业务逻辑规范

## 日志查询模块 (Home)
### 核心功能
- **搜索功能**: 支持关键词搜索和 SQL 查询
- **时间选择**: 绝对时间、相对时间、快速时间选择
- **数据可视化**: 直方图展示日志分布
- **虚拟表格**: 高性能日志数据展示
- **字段管理**: 动态字段显示和隐藏

### 状态管理
```typescript
interface HomePageState {
  searchParams: SearchParams;
  timeRange: TimeRange;
  selectedModule: string;
  tableData: LogData[];
  loading: boolean;
  error: string | null;
}
```

### 关键 Hooks
- `useHomePageState`: 页面状态管理
- `useUrlParams`: URL 参数同步
- `useAutoRefresh`: 自动刷新功能

## SQL 编辑器模块
### 核心功能
- **代码编辑**: Monaco Editor 集成
- **语法高亮**: SQL 语法支持
- **自动补全**: 数据库模式感知
- **查询执行**: 支持多种数据源
- **结果展示**: 表格和图表可视化
- **历史管理**: 查询历史记录

### 状态管理
```typescript
interface SQLEditorState {
  currentQuery: string;
  selectedDataSource: DataSource;
  queryResults: QueryResult[];
  executionHistory: QueryHistory[];
  isExecuting: boolean;
}
```

## 系统管理模块
### 数据源管理
- 支持多种数据库类型
- 连接测试和验证
- 权限控制

### Logstash 管理
- 配置管理
- 状态监控
- 日志收集规则

### 用户管理
- 用户权限控制
- 角色管理
- 操作审计

## AI 助手模块
### 核心功能
- **智能分析**: 日志异常检测
- **自然语言查询**: 转换为 SQL 查询
- **实时通信**: SSE 流式响应
- **上下文理解**: 基于当前页面状态

### 集成方式
```typescript
interface AIAssistantContext {
  currentPage: string;
  searchContext: SearchContext;
  userQuery: string;
  suggestions: AISuggestion[];
}
```

## 数据流管理
### 查询流程
1. 用户输入 → 参数验证 → API 调用
2. 数据获取 → 格式化 → 状态更新
3. UI 渲染 → 用户交互 → 状态变更

### 错误处理
- 网络错误重试
- 用户友好的错误提示
- 错误日志记录

### 性能优化
- 查询防抖
- 结果缓存
- 虚拟化渲染
- 分页加载

## 权限控制
### 页面级权限
- 路由守卫
- 菜单权限
- 功能权限

### 数据级权限
- 数据源访问权限
- 查询结果过滤
- 操作权限验证

## 配置管理
### 用户配置
- 界面偏好设置
- 查询默认参数
- 个性化配置

### 系统配置
- API 端点配置
- 功能开关
- 性能参数