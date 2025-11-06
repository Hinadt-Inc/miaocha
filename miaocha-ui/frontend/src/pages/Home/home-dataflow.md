# Home 模块与数据流梳理

> 目标：完整说明首页模块的职责边界、核心状态、跨组件数据流向、查询触发与副作用时序，以及刷新/分享/自动查询等场景的参数应用路径，帮助快速上手与定位问题。

## 模块总览

- 入口组件：`frontend/src/pages/Home/index.tsx`（`HomePage`）。
- 核心状态 Hook：`Home/hooks/useHomePageState.ts`（集中持有页面状态与 refs）。
- 业务编排 Hook：`Home/hooks/useBusinessLogic.ts`（统一初始化、参数应用、主查询节流与列/模块切换）。
- 数据请求 Hook：`Home/hooks/useDataRequests.ts`（日志明细、柱状图、模块列表、查询配置等请求）。
- URL 参数处理：`Home/hooks/useUrlParams.ts`（解析分享/回显参数，URL 清理与状态写入）。
- 组件：
  - `SearchBar`（关键词/SQL/时间/排序/触发查询等）：`Home/SearchBar/*`
  - `Sider`（模块选择、字段勾选、分布视图）：`Home/SiderModule/*`
  - `Log`（虚拟表格、展开行、柱状图）：`Home/LogModule/*`、`VirtualTable/index.tsx`
- 类型与常量：`Home/types.ts`、`Home/constants.ts`、`Home/utils.ts`。

## 单一事实源（Single Source of Truth）

- 页面级“查询参数”唯一可信来源：`useHomePageState.searchParams: ILogSearchParams`。
- 任何影响查询的输入（时间、关键词、SQL、排序、字段、模块等）最终都要汇总并写回 `searchParams`，再由业务层进行副作用（请求）。

## 核心状态

- 查询参数：`searchParams: ILogSearchParams`
  - 关键字段：`offset/pageSize/datasourceId/module/startTime/endTime/timeRange/timeGrouping/whereSqls/keywords/fields/sortFields/timeType/relativeStartOption/relativeEndOption`。
- 共享参数：`sharedParams: ISharedParams | null`
  - 来源：分享链接、刷新回显、本地缓存（`STORAGE_KEYS.SHARED_PARAMS`）。
  - 内容：关键词、SQL、时间范围、时间分组、字段列集合等。
- 其他主状态：
  - `moduleOptions` 模块列表；`selectedModule` 当前模块。
  - `logTableColumns` 所有列定义；`activeColumns` 已选择列；`commonColumns` 常用列。
  - `histogramData` 柱状图数据；`detailData` 日志列表数据。
  - `moduleQueryConfig` 模块查询配置（含时间字段、默认列、模板等）。
- 关键 refs：
  - `searchBarRef`、`siderRef` 组件方法调用；
  - `abortRef` 取消请求；`requestTimerRef` 节流/防抖计时器；
  - `lastCallParamsRef` 记录最近一次有效请求参数；`loadedConfigModulesRef` 已加载配置的模块集；
  - `isInitializingRef/processedUrlRef` 初始化与 URL 处理状态。

## 初始化流程（时序）

1. `HomePage` 挂载：构建 `useHomePageState` 全量状态与 refs。
2. 解析 URL/存储：`useUrlParams` 读取 `URL_PARAMS` 与 `STORAGE_KEYS`，组装 `sharedParams` 并写入状态；同时清理 URL 中的已消费参数（`URL_PARAMS_TO_CLEAN`）。
3. 拉取模块列表与初始配置：
   - `useDataRequests` 加载 `moduleOptions` 与默认 `moduleQueryConfig`。
   - 记录 `columnsLoaded` 与 `loadingQueryConfig`，确保后续“应用共享参数”不会早于配置加载完成。
4. 应用共享参数：`useBusinessLogic.applySharedParams` 分阶段、延时地将 `sharedParams` 映射到 `SearchBar` 与 `searchParams`：
   - 关键词/SQL；时间范围（绝对/相对）；时间分组；字段列集合；排序配置。
   - 使用 `SHARED_PARAMS_APPLY_DELAY` 与组件 `ref` 就绪检查，避免初始化抖动。
5. 恢复列选中状态：基于 `logTableColumns` 与共享字段集，恢复 `activeColumns`。
6. 首次主查询触发：`useBusinessLogic` 监听 `searchParams + moduleQueryConfig`，去抖后执行明细与柱状图请求。

## 查询触发与副作用

- 主触发点：
  - `SearchBar.onSearch`（点击“查询”、时间/关键词/SQL变更、刷新按钮）；
  - `Sider.onSearch`（字段/模块调整引起的查询）；
  - `Log.onSearchFromTable`（表格交互，如在列或排序变化时触发的查询）。
- 归并路径：所有入口会调用页面层的统一 `executeDataRequest`，它读取 `searchParams` 并并发请求：
  - 明细日志：`getDetailData.run(searchParams)`；
  - 柱状图：`getHistogramData.run(searchParams)`；
  - 必要时刷新列与分布：`getColumns.run(module)`、`getFieldDistributions.run(...)`。
- 节流/防抖：`REQUEST_DEBOUNCE_DELAY`（来源于 `Home/constants.ts`）在 `useBusinessLogic` 内控制请求频率与取消上一次未完成的调用。

## URL 参数与分享/刷新回显

- URL 参数键：`URL_PARAMS`（如 `TICKET/KEYWORDS/WHERE_SQLS/TIME_RANGE/START_TIME/END_TIME/MODULE/TIME_GROUPING`）。
- 处理策略：
  - 首次进入或分享链接：解析 URL → 组装为 `sharedParams` → 写入状态 → 清理 URL（避免污染后续导航）。
  - 刷新/回显：从 `sessionStorage/localStorage` 的 `STORAGE_KEYS.SHARED_PARAMS`、`STORAGE_KEYS.SEARCH_BAR_PARAMS` 读取 → 同上。
- 应用顺序（重要）：时间与模块优先，列集合与排序次之，关键词/SQL最后，以确保列/分布依赖模块配置时不出现“空列/错列”。

## 组件之间的交互关系

- SearchBar：
  - 入参：`searchParams/totalCount/columns/activeColumns/commonColumns/sortConfig/keywords/sqls/loading/sharedParams/hasAppliedSharedParams`。
  - 出参回调：`onSearch/onRefresh/onSqlsChange/setKeywords/setSqls/onRemoveSql/setWhereSqlsFromSider/refreshFieldDistributions`。
  - 提供 `ref` 方法：`addSql/removeSql/setTimeOption/setTimeGroup/autoRefresh`。
- Sider：
  - 入参：`searchParams/modules/moduleQueryConfig/selectedModule/activeColumns/commonColumns`。
  - 出参回调：`onSearch/onChangeColumns/onActiveColumnsChange/onSelectedModuleChange/onCommonColumnsChange/setWhereSqlsFromSider/onColumnsLoaded`。
- Log/VirtualTable：
  - 入参：`histogramData/detailData/searchParams/dynamicColumns/sqls/whereSqlsFromSider/moduleQueryConfig`。
  - 出参回调：`onChangeColumns/onSearchFromTable/onSortChange`。
  - 排序变化：`VirtualTable.handleTableChange` 解析 `sorter` → 组装 `SortConfig` → 回调 `onSortChange` → 写入 `searchParams.sortFields` → 触发主查询。

## 自动定时查询

- 触发位置：`SearchBarRef.autoRefresh`（如开启“自动刷新”）或业务层定时器。
- 行为：定时调用 `onRefresh` → 更新 `searchParams` 的时间窗口（相对时间时重新计算）→ 执行 `executeDataRequest`。
- 注意：避免与手动查询/URL 参数应用产生竞争，通常需要 `isInitialized/hasAppliedSharedParams` 作为保护条件。

## 字段列选择与恢复

- 来源：`moduleQueryConfig` 的默认列 + 用户勾选的 `activeColumns`。
- 恢复策略：
  - 当存在共享参数的字段集合时，用其与 `logTableColumns` 求交集，过滤无效列；
  - `columnsLoaded` 标记确保列数据已就绪再应用，避免“应用前列未到位”。
- 变更流：
  - `Sider.setActiveColumns` 或 `Log.onChangeColumns` → 写入 `activeColumns` → 同步到 `searchParams.fields` → 触发查询。

## 共享参数应用细则

- 关键词/SQL：更新 `keywords/sqls` 并同步 `searchParams.whereSqls`；`removeSql` 时做差集、保持 `SearchBar` 与 `searchParams` 一致。
- 时间：绝对时间（`startTime/endTime`）与相对时间（`relativeStartOption/relativeEndOption/timeRange`）互斥；`setTimeOption` 会推动 `searchParams.timeType` 与时间字段刷新。
- 时间分组：`timeGrouping` 应用于柱状图与分布统计；
- 排序：`sortFields` 来自 `VirtualTable` 的 `SortConfig`；变更后明细与柱状图遵循同一排序字段（若后端支持）。

## 触发条件一览（便于排查重复查询）

- `searchParams` 任何关键字段变化（模块、时间、关键词、SQL、列、排序）→ 防抖后主查询。
- `moduleQueryConfig` 加载完成或变化 → 若 `searchParams` 已就绪，触发主查询。
- `sharedParams` 应用阶段完成 → 触发一次主查询。
- `Sider` 的字段/模块变更 → 触发主查询。
- `SearchBar` 的查询/刷新 → 触发主查询。
- `VirtualTable` 的排序变化 → 触发主查询。

## 本地存储与清理

- 键集合：`STORAGE_KEYS` → `SHARED_PARAMS/OAUTH_PROVIDER/SEARCH_BAR_PARAMS`。
- URL 清理：`URL_PARAMS_TO_CLEAN` 在 `useUrlParams` 中消费后移除，避免影响浏览器刷新与分享复制。

## 异常与保护

- 初始化竞争：通过 `isInitializingRef/hasAppliedSharedParams/columnsLoaded/loadingQueryConfig` 等保护，避免“配置未就位先应用参数”。
- 请求取消：`abortRef` 在新的查询触发时取消未完成的请求，减少资源浪费与竞态。
- 空列/错列：字段应用前必须以 `logTableColumns` 为全集做交集过滤。

## 调试建议（定位重复查询/错配）

- 打点：在 `useBusinessLogic` 的防抖入口与 `executeDataRequest` 前后打日志，记录 `lastCallParamsRef` 与变更源。
- 观察条件：统一只以 `searchParams` 为副作用依赖，减少多处 `useEffect` 独立监听。
- 约束：模块切换时先确保 `moduleQueryConfig` 完成，再应用共享列/排序。

## 文件指引（快速索引）

- `Home/index.tsx`：页面 orchestrator，集中触发与清理。
- `Home/hooks/useHomePageState.ts`：状态与 refs 的唯一持有者。
- `Home/hooks/useBusinessLogic.ts`：参数应用、列恢复与主查询的调度中心。
- `Home/hooks/useDataRequests.ts`：所有请求的统一封装。
- `Home/constants.ts`：默认查询参数、URL/存储键、时间与防抖常量。
- `Home/types.ts`：`ILogSearchParams/ISharedParams/SortConfig/...` 类型定义。
- `VirtualTable/index.tsx`：排序变更上报 `onSortChange` 的实现位置（`handleTableChange`）。
- `Home/SiderModule/hooks/index.ts`：时间字段推断 `determineTimeField`。

## 常见时序示例

- 示例：用户在 SearchBar 修改时间 → 点击查询：
  1. `SearchBar.setTimeOption` 更新内部时间状态与 `searchParams`；
  2. 回调 `onSearch` → 页面层 `executeDataRequest`；
  3. `useBusinessLogic` 防抖合流 → 取消上次请求（如在途）→ 并发拉取明细与柱状图；
  4. `VirtualTable` 与柱状图组件接收新数据渲染。

## 优化建议（不改变现状的前提下）

- “单依赖”策略：副作用只监听 `searchParams` 与少量 gate（`moduleQueryConfig/columnsLoaded`），其它输入写回到 `searchParams`，避免多源监听。
- 参数应用次序固化：模块/时间 → 列 → 排序 → 关键词/SQL，统一在 `useBusinessLogic` 内完成。
- 统一取消：`abortRef` 与 `requestTimerRef` 在同一层统一管理，组件内部不再各自节流。
- 列与分布的一致性：字段集合变化时，优先触发分布刷新 `refreshFieldDistributions`，避免 UI 与统计不一致。

---

以上梳理围绕当前代码结构与职责分配，若后续需求扩展（新增筛选项/多数据源/多视图联动），建议继续沿用“写回 `searchParams` → 统一副作用”的主轴，确保数据流始终清晰、可控、可调试。
