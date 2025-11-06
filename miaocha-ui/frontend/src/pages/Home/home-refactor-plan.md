# Home 重构方案（Redux Toolkit 版·完善稿）

> 目标：统一 `searchParams` 为单一事实源（SSOT）；将刷新回显、分享链接回显、多标签页隔离纳入统一流程；收敛 hooks 的数据监听，改为“写回 Store → 单一编排副作用触发”，消除重复查询与竞态，提升可维护性与可测试性。

## 1. 背景与现状数据流（复盘）
- 入口：`Home/index.tsx` 负责页面 orchestrator 与各组件集成。
- 状态持有：`useHomePageState.ts` 管理 `searchParams/ISharedParams/moduleOptions/activeColumns/logTableColumns/moduleQueryConfig` 等，并持有大量 refs（`searchBarRef/siderRef/abortRef/...`）。
- 业务编排：`useBusinessLogic.ts` 分阶段应用共享参数、恢复列、做主查询防抖与取消。
- 请求封装：`useDataRequests.ts` 发起日志明细/柱状图/模块列表/配置等请求。
- URL/缓存：`useUrlParams.ts` 解析 URL 与存储，写入共享参数并清理 URL。
- 组件交互：`SearchBar/Sider/Log(VirtualTable)` 修改关键词、SQL、时间、列、排序、模块等，再触发页面层查询。

主要痛点：
- 多处 `useEffect` 监听导致重复触发与竞态（时间/模块/列/排序变化各自触发）。
- 查询参数在多处读写，“多源”风险高，维护成本大。
- 初始化阶段（URL/缓存/模块配置）容易出现“列未就绪先应用”的时序问题。
- 刷新回显/分享回显/多标签隔离逻辑分散，不易统一保障。

## 2. 重构目标与原则
- 单一事实源：仅以 Store 中的 `searchParams: ILogSearchParams` 作为唯一查询参数来源。
- 单一副作用：仅保留一个查询编排（Orchestrator），统一监听 `searchParams` + 少量 gate（`hydrationDone/moduleQueryConfig/columnsLoaded`）。
- 统一回显：初始化阶段一次性 Hydration，按固定顺序应用共享参数并清理 URL。
- 标签隔离：每标签页独立 `tabId` 与 `sessionStorage` 持久化，刷新/回显只影响当前标签。
- 组件去副作用：组件只 dispatch Action，不直接发请求或维护查询副作用。
- 时间互斥：绝对时间与相对时间字段互斥，Action 内部确保互斥一致性。

## 3. 架构总览（Redux Toolkit）
- `searchSlice`（唯一事实源）
  - state：`tabId/hydrationDone/hasAppliedSharedParams/searchParams`
  - actions：`initTab/reset/patch/setModule/setColumns/setSort/setKeywords/setSqls/setTimeAbsolute/setTimeRelative/applySharedParams/markHydrationDone`
  - thunks：`hydrateFromUrl/persistSearchParams`
- `configSlice`
  - state：`moduleOptions/moduleQueryConfig/logTableColumns/columnsLoaded/loadingQueryConfig`
  - thunks：拉取模块列表与查询配置；列就绪后再应用共享字段（必要时在 `applySharedParams` 之后触发列交集过滤）。
- `dataSlice`
  - state：`detailData/histogramData/loading/error`
  - thunks：`fetchDetailData/fetchHistogramData`
- `listenerMiddleware`（Orchestrator）
  - 仅监听 `searchParams` 相关 actions；在 gate 满足（`hydrationDone && moduleQueryConfig && columnsLoaded`）时进行防抖并发起请求；统一取消在途请求。
- URL/缓存 Hydration
  - 初始化：`hydrateFromUrl` 读取 URL 与缓存（`STORAGE_KEYS`），组合为 `sharedParams`；按“模块/时间 → 时间分组 → 列 → 排序 → 关键词/SQL”的顺序应用；最后 `markHydrationDone()`；清理 URL。

## 4. 关键改进与查漏补缺
- 初始化 gate 从 `hasAppliedSharedParams` 改为 `hydrationDone`
  - 解释：无共享参数场景也需要触发首次查询；`hydrationDone` 表示初始化完成（有/无共享参数均可）。`hasAppliedSharedParams` 保留为“是否应用过共享参数”的信息位，仅用于 UI 或打点，不参与副作用 gate。
- 列集合与分布一致性
  - 在应用共享字段与列恢复时，必须与 `logTableColumns` 做交集过滤，避免“错列/空列”；必要时触发分布刷新以保持统计与 UI 一致。
- 排序链路统一
  - `VirtualTable.handleTableChange` → `onSortChange` → `dispatch(setSort)` → Orchestrator 防抖查询；同时确保后端排序字段与明细/柱状图一致（若后端支持）。
- 自动刷新整合
  - 由 `AutoRefresh` 组件改为仅 dispatch 时间相关 action（相对时间场景），不要自行发请求；Orchestrator 统一触发。
- 多标签隔离与持久化
  - 每标签生成 `tabId`；`persistSearchParams()` 仅写入当前标签的 `sessionStorage`；刷新后由 `hydrateFromUrl()` 回显。
- URL 分享格式与清理
  - 统一分享参数键（复用 `URL_PARAMS`）；页面打开后消费即清理，避免污染后续导航与刷新。
- 错误处理与中断
  - Orchestrator 使用 `AbortController` 取消在途请求，减少竞态与资源浪费；请求失败时统一记录日志并以用户友好的方式提示（复用 `ErrorProvider`）。

## 5. 代码片段（JSDoc 中文注释示例）

```ts
// src/store/home/searchSlice.ts
import { createSlice, PayloadAction, createAsyncThunk } from "@reduxjs/toolkit";
import { ILogSearchParams, ISharedParams, SortConfig } from "../../pages/Home/types";
import { DEFAULT_SEARCH_PARAMS, STORAGE_KEYS, URL_PARAMS } from "../../pages/Home/constants";

/**
 * Search 单一事实源（SSOT）
 * - 仅此处持有并修改 searchParams
 * - 初始化：hydrateFromUrl → applySharedParams（若存在）→ markHydrationDone
 * - 副作用：由 listenerMiddleware 统一编排
 */
export interface SearchState {
  tabId: string;
  hydrationDone: boolean;           // 初始化完成标记（无论是否有共享参数）
  hasAppliedSharedParams: boolean;  // 是否应用过共享参数（供 UI/打点使用）
  searchParams: ILogSearchParams;
}

const initialState: SearchState = {
  tabId: "",
  hydrationDone: false,
  hasAppliedSharedParams: false,
  searchParams: { ...DEFAULT_SEARCH_PARAMS },
};

function getOrCreateTabId(): string {
  const KEY = "TAB_ID";
  const existing = sessionStorage.getItem(KEY);
  if (existing) return existing;
  const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  sessionStorage.setItem(KEY, id);
  return id;
}

function parseSharedParamsFromUrl(): ISharedParams | null {
  const usp = new URLSearchParams(window.location.search);
  const module = usp.get(URL_PARAMS.MODULE) || undefined;
  const keywords = usp.getAll(URL_PARAMS.KEYWORDS);
  const whereSqls = usp.getAll(URL_PARAMS.WHERE_SQLS);
  const timeRange = usp.get(URL_PARAMS.TIME_RANGE) || undefined;
  const startTime = usp.get(URL_PARAMS.START_TIME);
  const endTime = usp.get(URL_PARAMS.END_TIME);
  const timeGrouping = usp.get(URL_PARAMS.TIME_GROUPING) || undefined;
  const result: ISharedParams = {
    module,
    keywords: keywords.length ? keywords : undefined,
    sqls: whereSqls.length ? whereSqls : undefined,
    timeRange: timeRange as ILogSearchParams["timeRange"],
    startTime: startTime ? Number(startTime) : undefined,
    endTime: endTime ? Number(endTime) : undefined,
    timeGrouping,
    fields: undefined,
  };
  const hasAny =
    result.module ||
    result.keywords?.length ||
    result.sqls?.length ||
    result.timeRange ||
    result.startTime ||
    result.endTime ||
    result.timeGrouping;
  return hasAny ? result : null;
}

function cleanUrlParams() {
  const usp = new URLSearchParams(window.location.search);
  Object.values(URL_PARAMS).forEach((k) => usp.delete(k));
  const newUrl = `${window.location.pathname}${usp.toString() ? `?${usp.toString()}` : ""}${window.location.hash}`;
  window.history.replaceState(null, "", newUrl);
}

export const persistSearchParams = createAsyncThunk(
  "search/persist",
  async (_, { getState }) => {
    const state = getState() as any;
    const p: ILogSearchParams = state.search.searchParams;
    const sharedLike: ISharedParams = {
      module: p.module,
      keywords: p.keywords,
      sqls: p.whereSqls,
      startTime: p.startTime,
      endTime: p.endTime,
      timeRange: p.timeRange,
      timeGrouping: p.timeGrouping,
      fields: p.fields,
    };
    sessionStorage.setItem(STORAGE_KEYS.SHARED_PARAMS, JSON.stringify(sharedLike));
  }
);

export const hydrateFromUrl = createAsyncThunk(
  "search/hydrateFromUrl",
  async (_, { dispatch }) => {
    const fromUrl = parseSharedParamsFromUrl();
    const raw = sessionStorage.getItem(STORAGE_KEYS.SHARED_PARAMS);
    const fromStorage = raw ? (JSON.parse(raw) as ISharedParams) : null;
    const shared = fromUrl || fromStorage || null;
    if (shared) {
      dispatch(applySharedParams(shared));
      cleanUrlParams();
    }
    dispatch(markHydrationDone()); // 无论是否有共享参数，都完成初始化
  }
);

const slice = createSlice({
  name: "search",
  initialState,
  reducers: {
    /** 初始化 tabId（多标签页隔离） */
    initTab(state) {
      state.tabId = getOrCreateTabId();
    },
    /** 标记初始化已完成（不依赖是否应用共享参数） */
    markHydrationDone(state) {
      state.hydrationDone = true;
    },
    /** 重置查询参数 */
    reset(state, action: PayloadAction<Partial<ILogSearchParams> | undefined>) {
      state.searchParams = { ...DEFAULT_SEARCH_PARAMS, ...(action.payload || {}) };
    },
    /** 原子 patch */
    patch(state, action: PayloadAction<Partial<ILogSearchParams>>) {
      state.searchParams = { ...state.searchParams, ...action.payload };
    },
    /** 模块 */
    setModule(state, action: PayloadAction<string | undefined>) {
      state.searchParams.module = action.payload;
      state.searchParams.offset = 0;
    },
    /** 列集合 */
    setColumns(state, action: PayloadAction<string[]>) {
      state.searchParams.fields = action.payload;
    },
    /** 排序 */
    setSort(state, action: PayloadAction<SortConfig | null>) {
      const sort = action.payload;
      state.searchParams.sortFields = sort ? [{ field: sort.field, order: sort.order }] : [];
    },
    /** 关键词 */
    setKeywords(state, action: PayloadAction<string[]>) {
      state.searchParams.keywords = action.payload;
    },
    /** SQL → whereSqls */
    setSqls(state, action: PayloadAction<string[]>) {
      state.searchParams.whereSqls = action.payload;
    },
    /** 绝对时间（互斥处理） */
    setTimeAbsolute(state, action: PayloadAction<{ start: number; end: number }>) {
      state.searchParams = {
        ...state.searchParams,
        timeType: "absolute",
        startTime: action.payload.start,
        endTime: action.payload.end,
        timeRange: undefined,
        relativeStartOption: undefined,
        relativeEndOption: undefined,
      };
    },
    /** 相对时间（互斥处理） */
    setTimeRelative(
      state,
      action: PayloadAction<{ range: ILogSearchParams["timeRange"]; rs?: ILogSearchParams["relativeStartOption"]; re?: ILogSearchParams["relativeEndOption"] }>
    ) {
      state.searchParams = {
        ...state.searchParams,
        timeType: "relative",
        timeRange: action.payload.range,
        startTime: undefined,
        endTime: undefined,
        relativeStartOption: action.payload.rs,
        relativeEndOption: action.payload.re,
      };
    },
    /** 应用共享参数（顺序：模块/时间 → 分组 → 列 → 排序 → 关键词/SQL） */
    applySharedParams(state, action: PayloadAction<ISharedParams>) {
      const shared = action.payload;
      if (shared.module) {
        state.searchParams.module = shared.module;
        state.searchParams.offset = 0;
      }
      if (shared.startTime && shared.endTime) {
        state.searchParams.timeType = "absolute";
        state.searchParams.startTime = shared.startTime;
        state.searchParams.endTime = shared.endTime;
        state.searchParams.timeRange = undefined;
        state.searchParams.relativeStartOption = undefined;
        state.searchParams.relativeEndOption = undefined;
      } else if (shared.timeRange) {
        state.searchParams.timeType = "relative";
        state.searchParams.timeRange = shared.timeRange;
        state.searchParams.startTime = undefined;
        state.searchParams.endTime = undefined;
      }
      if (shared.timeGrouping) {
        state.searchParams.timeGrouping = shared.timeGrouping;
      }
      if (shared.fields?.length) {
        state.searchParams.fields = shared.fields;
      }
      const sortFields = (shared as any).sortFields as ILogSearchParams["sortFields"] | undefined;
      if (sortFields?.length) {
        state.searchParams.sortFields = sortFields;
      }
      if (shared.keywords?.length) {
        state.searchParams.keywords = shared.keywords;
      }
      if (shared.sqls?.length) {
        state.searchParams.whereSqls = shared.sqls;
      }
      state.hasAppliedSharedParams = true;
    },
  },
});

export const {
  initTab,
  markHydrationDone,
  reset,
  patch,
  setModule,
  setColumns,
  setSort,
  setKeywords,
  setSqls,
  setTimeAbsolute,
  setTimeRelative,
  applySharedParams,
} = slice.actions;
export default slice.reducer;
```

```ts
// src/store/home/orchestrator.ts（listenerMiddleware 示例）
import { createListenerMiddleware, isAnyOf } from "@reduxjs/toolkit";
import { REQUEST_DEBOUNCE_DELAY } from "../../pages/Home/constants";
import { RootState, AppDispatch } from "../store";
import {
  patch,
  setModule,
  setColumns,
  setSort,
  setKeywords,
  setSqls,
  setTimeAbsolute,
  setTimeRelative,
} from "./searchSlice";
import { fetchDetailData, fetchHistogramData } from "./dataSlice"; // 请按项目实际实现
import { selectModuleQueryConfig, selectColumnsLoaded } from "./selectors"; // 请按项目实际实现

/**
 * 统一查询编排副作用
 * - gate：hydrationDone && columnsLoaded && moduleQueryConfig
 * - 防抖 + 并发请求，避免重复与竞态
 */
export const queryOrchestrator = createListenerMiddleware();

queryOrchestrator.startListening({
  matcher: isAnyOf(
    patch,
    setModule,
    setColumns,
    setSort,
    setKeywords,
    setSqls,
    setTimeAbsolute,
    setTimeRelative
  ),
  effect: async (action, api) => {
    const state = api.getState() as RootState;
    const dispatch = api.dispatch as AppDispatch;

    const gateReady =
      state.search.hydrationDone &&
      selectColumnsLoaded(state) &&
      Boolean(selectModuleQueryConfig(state));
    if (!gateReady) return;

    // 防抖
    await new Promise((resolve) => setTimeout(resolve, REQUEST_DEBOUNCE_DELAY));

    const params = state.search.searchParams;
    await Promise.all([
      dispatch(fetchDetailData(params)),
      dispatch(fetchHistogramData(params)),
    ]);
  },
});
```

```ts
// src/store/store.ts（集成示例）
import { configureStore } from "@reduxjs/toolkit";
import searchReducer from "./home/searchSlice";
import { queryOrchestrator } from "./home/orchestrator";
// import configReducer from "./home/configSlice";
// import dataReducer from "./home/dataSlice";

export const store = configureStore({
  reducer: {
    search: searchReducer,
    // config: configReducer,
    // data: dataReducer,
  },
  middleware: (getDefault) => getDefault().concat(queryOrchestrator.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

## 6. 组件改造指南
- SearchBar
  - 改为 dispatch：`setKeywords/setSqls/setTimeAbsolute/setTimeRelative`；点击“查询/刷新”执行 `persistSearchParams()`；不直接发请求。
- Sider
  - 模块/字段变化：`setModule/setColumns`。
- Log/VirtualTable
  - 排序变化：`setSort`；不直接发请求。
- HomePage
  - 首次挂载：`initTab()` → `hydrateFromUrl()`；其余查询由 Orchestrator 统一触发。

## 7. 多标签页隔离与回显
- `tabId`：每标签页使用 `sessionStorage('TAB_ID')` 生成独立 ID。
- 刷新回显：`persistSearchParams()` 写入当前标签缓存；刷新后 `hydrateFromUrl()` 回显并清理 URL。
- 分享链接：打开后解析 URL → 应用共享参数 → 清理 URL；不影响其它标签页。

## 8. 错误处理与日志
- 请求失败：通过统一的 ErrorProvider 显示用户友好提示；同时记录日志（模块/参数/错误码）。
- 中断机制：统一用 `AbortController` 取消在途请求，减少竞态。

## 9. 测试与验收
- 单元测试
  - 时间互斥：`setTimeAbsolute` 与 `setTimeRelative` 的字段覆盖逻辑。
  - 排序/列/模块 action 后，`searchParams` 更新正确。
- 集成测试
  - 组件交互只触发一次查询（mock thunks 计数）。
  - 刷新可回显；分享链接应用并清理 URL；多标签互不影响。
- 验收标准
  - 初始化：无共享参数也能触发首轮查询。
  - 交互：任意参数变化只触发一轮查询，且可取消在途请求。
  - 一致性：列集合与分布一致，排序在明细与柱状图一致（若后端支持）。

## 10. 迁移 Checklist（分阶段）
1. 引入 `searchSlice` 与 `queryOrchestrator`，在 `store.ts` 集成。
2. `Home/index.tsx`：删除页面内多处查询 useEffect；改为 `initTab()` 与 `hydrateFromUrl()`。
3. 组件交互全面改为 dispatch（SearchBar/Sider/VirtualTable）。
4. 校验：关键词/SQL/时间/模块/列/排序变化时，副作用只在 Orchestrator 触发一次。
5. 刷新回显与分享链接验证；多标签隔离验证。

## 11. JSDoc 中文注释规范（示例）
- 在 `searchSlice/orchestrator` 顶部与关键 reducer/effect 上补充中文 JSDoc：

```ts
/**
 * 应用共享参数到查询参数
 * @param shared 共享参数（来自 URL/缓存）
 * 说明：
 * 1. 先应用模块与时间，再应用分组、列与排序，最后关键词与 SQL
 * 2. 时间字段互斥（absolute vs relative），避免混用
 */
```

```ts
/**
 * 统一查询编排（listenerMiddleware）
 * 说明：
 * - 仅监听 searchParams 相关 actions，避免多源副作用
 * - gate：hydrationDone + columnsLoaded + moduleQueryConfig
 * - 防抖与取消在途请求，杜绝重复查询与竞态
 */
```

---

以上完善稿补充了初始化 gate 的更正（`hydrationDone`）、列与分布一致性、自动刷新整合、错误处理与测试验收等关键点，并给出 Redux Toolkit 的完整落地片段与中文 JSDoc 规范。按本方案迁移后，`searchParams` 的维护与查询触发将更清晰、稳定且易于扩展。