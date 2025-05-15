import { useState, useCallback, useEffect, useMemo, useRef } from 'react';
import { Modal } from 'antd';
import { throttle } from '@/utils/logDataHelpers';
import { useRequest } from 'ahooks';
import { useLogData } from '@/hooks/useLogData';
import { useFilters } from '@/hooks/useFilters';
import SearchBar from './SearchBar';
import Log from './Log';
import SiderComponent from './Sider';
import * as api from '@/api/permission';
import { getTableColumns, searchLogs } from '@/api/logs';
import styles from './index.module.less';

const HomePage = () => {
  // 模块名称列表，用于字段选择等组件
  const [moduleNames, setModuleNames] = useState<IStatus[]>([]);
  const [timeRange, setTimeRange] = useState<[string, string] | undefined>(undefined);
  const [timeRangePreset, setTimeRangePreset] = useState<string | undefined>(undefined);
  // 日志数据
  const [log, setLog] = useState<ISearchLogsResponse | null>(null);
  // 搜索参数
  const [searchParams, setSearchParams] = useState<ISearchLogsParams>({
    pageSize: 20,
    datasourceId: undefined,
    module: undefined,
    // timeRange: 'yesterday',
  });

  // 查询日志
  const fetchLog = useRequest(searchLogs, {
    manual: true,
    onSuccess: (res) => {
      const { rows } = res;
      // 为每条记录添加唯一ID
      (rows || []).map((item, index) => {
        item.key = `${Date.now()}-${index}`;
      });
      setLog(res);
    },
  });

  useEffect(() => {
    // 只判断 datasourceId，因为这是查询的必要参数
    if (searchParams.datasourceId && searchParams.module) {
      fetchLog.run(searchParams);
    }
  }, [searchParams]);

  // 获取模块名称
  const fetchModuleNames = useRequest(api.getMyModules, {
    onSuccess: (res) => {
      const target: IStatus[] = [];
      res?.forEach((item) => {
        const { modules, datasourceId, datasourceName } = item;
        const sub = {};
        Object.assign(sub, {
          label: String(datasourceName),
          value: String(datasourceId),
        });
        const children: IStatus[] = [];
        if (modules?.length > 0) {
          const names = modules?.map((m: any) => ({
            value: String(m.moduleName),
            label: String(m.moduleName),
          }));
          children.push(...names);
        }
        Object.assign(sub, {
          children,
        });
        target.push(sub);
      });

      // 如果 searchParams 中没有 datasourceId，则使用第一个数据源的 ID
      if (target[0] && (!searchParams.datasourceId || !searchParams.module)) {
        setSearchParams((prev) => ({
          ...prev,
          datasourceId: Number(target[0].value),
          module: target[0]?.children[0]?.value,
        }));
      }
      setModuleNames(target);
    },
  });

  const onSearch = (params: ISearchLogsParams) => {
    setSearchParams({
      datasourceId: searchParams.datasourceId,
      module: searchParams.module,
      pageSize: searchParams.pageSize,
      // timeRange: searchParams.timeRange, // todo
      ...params,
    });
  };

  // 旧的 ================================
  const [selectedFields, setSelectedFields] = useState<string[]>(['log_time', 'message']);
  const [viewMode, setViewMode] = useState<'table' | 'json'>('table');
  const [searchQuery, setSearchQuery] = useState('');
  const [whereSql, setWhereSql] = useState('');
  const [showHistogram, setShowHistogram] = useState(true);
  const [timeDisplayText, setTimeDisplayText] = useState<string | undefined>(undefined);
  const [showTimePicker, setShowTimePicker] = useState(false);
  const [selectedTable, setSelectedTable] = useState<string>('');
  const lastAddedFieldRef = useRef<string | null>(null);
  const lastRemovedFieldRef = useRef<string | null>(null);
  const [renderKey, setRenderKey] = useState<number>(0); // 用于强制更新UI的key

  // 将状态放入ref以减少不必要的重渲染
  const availableTablesRef = useRef<
    Array<{
      datasourceId: number;
      datasourceName: string;
      databaseName: string;
      tables: Array<{
        tableName: string;
        tableComment: string;
        columns: Array<{
          columnName: string;
          dataType: string;
          columnComment: string;
          isPrimaryKey: boolean;
          isNullable: boolean;
        }>;
      }>;
    }>
  >([]);

  const [availableFields, setAvailableFields] = useState<
    Array<{ columnName: string; dataType: string }>
  >([]);
  const prevSelectedTable = useRef<string>('');

  // 缓存表格数据引用
  const tableDataRef = useRef<any[]>([]);

  // 新增数据加载状态
  const [tableLoading, setTableLoading] = useState<boolean>(false);
  const [fieldLoading, setFieldLoading] = useState<boolean>(false);

  // 添加请求缓存
  const tableColumnsCache = useRef<Record<string, any[]>>({});

  // 将表格加载状态与全局加载状态集成
  // useEffect(() => {
  //   if (tableLoading && !isGlobalLoading) {
  //     startLoading('tableLoading');
  //   } else if (!tableLoading && isGlobalLoading) {
  //     endLoading('tableLoading');
  //   }
  // }, [tableLoading, isGlobalLoading, startLoading, endLoading]);

  // 将字段加载状态与全局加载状态集成
  // useEffect(() => {
  //   if (fieldLoading && !isGlobalLoading) {
  //     startLoading('fieldLoading');
  //   } else if (!fieldLoading && isGlobalLoading) {
  //     endLoading('fieldLoading');
  //   }
  // }, [fieldLoading, isGlobalLoading, startLoading, endLoading]);

  // 获取表字段，添加缓存和错误处理
  const fetchTableColumns = useCallback(async (tableIdentifier: string) => {
    if (!tableIdentifier || tableIdentifier === prevSelectedTable.current) return;

    setFieldLoading(true);

    try {
      // 检查缓存中是否已存在该表的字段
      if (tableColumnsCache.current[tableIdentifier]) {
        setAvailableFields(tableColumnsCache.current[tableIdentifier]);
        setFieldLoading(false);
        prevSelectedTable.current = tableIdentifier;
        return;
      }

      const [datasourceId, tableName] = tableIdentifier.split('-');
      const columns = (await getTableColumns(datasourceId, tableName)) as Array<{
        columnName: string;
        dataType: string;
      }>;

      // 更新缓存
      tableColumnsCache.current[tableIdentifier] = columns;
      setAvailableFields(columns);
      prevSelectedTable.current = tableIdentifier;
    } catch (error) {
      console.error('获取表字段失败:', error);
      // 显示友好的错误提示
      Modal.error({
        title: '获取字段失败',
        content: '无法获取表字段，请检查网络连接或联系管理员',
      });
    } finally {
      setFieldLoading(false);
    }
  }, []);

  // 使用节流函数优化表选择
  // const throttledFetchColumns = useMemo(
  //   () => throttle(fetchTableColumns, 500),
  //   [fetchTableColumns],
  // );

  // 表选择变化处理
  // const handleTableChange = useCallback(
  //   (tableId: string) => {
  //     setSelectedTable(tableId);
  //     if (tableId !== prevSelectedTable.current) {
  //       throttledFetchColumns(tableId);
  //     }
  //   },
  //   [throttledFetchColumns],
  // );

  // 设置默认时间范围为最近15分钟
  // useEffect(() => {
  //   if (!timeRange) {
  //     const now = new Date();
  //     const fifteenMinutesAgo = new Date(now.getTime() - 15 * 60 * 1000);

  //     setTimeRange([
  //       fifteenMinutesAgo.toISOString().substring(0, 19).replace('T', ' '),
  //       now.toISOString().substring(0, 19).replace('T', ' '),
  //     ]);
  //     setTimeRangePreset('last_15m');
  //   }
  // }, []);

  // 使用useMemo优化搜索参数构建，减少不必要的对象创建
  const [timeGrouping, setTimeGrouping] = useState<'minute' | 'hour' | 'day' | 'month'>('minute');

  // 获取表权限数据，优化为仅在组件挂载时执行一次
  useEffect(() => {
    // const fetchTablePermissions = async () => {
    //   setTableLoading(true);
    //   try {
    //     const data = await getMyTablePermissions();
    //     const transformedData = data.map((ds) => ({
    //       datasourceId: ds.datasourceId,
    //       datasourceName: ds.datasourceName,
    //       databaseName: ds.databaseName,
    //       tables: ds.tables.map((table: TablePermission) => ({
    //         tableName: table.tableName,
    //         tableComment: table.tableComment || '',
    //         columns: (table.columns || []).map((col: TableColumn) => ({
    //           columnName: col.columnName,
    //           dataType: col.dataType,
    //           columnComment: col.columnComment || '',
    //           isPrimaryKey: col.isPrimaryKey || false,
    //           isNullable: col.isNullable || false,
    //         })),
    //       })),
    //     }));
    //     // 使用ref存储数据，减少重渲染
    //     availableTablesRef.current = transformedData;
    //     setRenderKey((prev) => prev + 1);
    //     // 默认选择第一个数据源和第一个表
    //     if (data.length > 0 && data[0].tables.length > 0) {
    //       const defaultTable = `${data[0].datasourceId}-${data[0].tables[0].tableName}`;
    //       setSelectedTable(defaultTable);
    //       throttledFetchColumns(defaultTable);
    //     }
    //   } catch (error) {
    //     console.error('获取表权限失败:', error);
    //     Modal.error({
    //       title: '获取表权限失败',
    //       content: '无法获取表权限信息，请检查网络连接或联系管理员',
    //     });
    //   } finally {
    //     setTableLoading(false);
    //   }
    // };
    // fetchTablePermissions();
  }, []);

  // 使用优化的 useLogData 钩子
  // const {
  //   tableData,
  //   loading,
  //   hasMore,
  //   loadMoreData,
  //   resetData,
  //   distributionData = [],
  //   totalCount,
  // } = useLogData({
  //   ...searchParams,
  //   tableName: selectedTable ? selectedTable.split('-')[1] : '',
  //   datasourceId: selectedTable ? Number(selectedTable.split('-')[0]) : 1,
  //   fields: selectedFields,
  // });

  // 将数据加载状态与全局加载状态集成
  // useEffect(() => {
  //   if (loading && !isGlobalLoading) {
  //     startLoading('dataLoading');
  //   } else if (!loading && isGlobalLoading) {
  //     // 延迟结束加载状态，避免闪烁
  //     const timer = setTimeout(() => {
  //       endLoading('dataLoading');
  //     }, 300);
  //     return () => clearTimeout(timer);
  //   }
  // }, [loading, isGlobalLoading, startLoading, endLoading]);

  // 优化 useFilters 钩子的使用
  // const {
  //   showFilterModal,
  //   setShowFilterModal,
  //   selectedFilterField,
  //   handleFilterFieldChange,
  //   addFilter,
  // } = useFilters();

  // 优化字段选择逻辑，使用带记忆的回调
  // const toggleFieldSelection = useCallback((fieldName: string) => {
  //   setSelectedFields((prev) => {
  //     if (prev.includes(fieldName)) {
  //       lastRemovedFieldRef.current = fieldName;
  //       lastAddedFieldRef.current = null;
  //       return prev.filter((f) => f !== fieldName);
  //     } else {
  //       lastAddedFieldRef.current = fieldName;
  //       lastRemovedFieldRef.current = null;
  //       return [...prev, fieldName];
  //     }
  //   });

  //   // 使用setTimeout清除动画状态
  //   setTimeout(() => {
  //     lastAddedFieldRef.current = null;
  //     lastRemovedFieldRef.current = null;
  //     // 强制更新以反映新的ref值
  //     setRenderKey((prev) => prev + 1);
  //   }, 2000);
  // }, []);

  // 优化滚动逻辑，添加节流机制避免频繁触发
  // const handleScroll = useCallback(
  //   throttle((e: React.UIEvent<HTMLDivElement>) => {
  //     if (loading || !hasMore) return;

  //     const element = e.currentTarget;
  //     if (!element) return;

  //     const scrollTop = element.scrollTop;
  //     const scrollHeight = element.scrollHeight;
  //     const clientHeight = element.clientHeight;

  //     // 当滚动到底部前200px时开始加载更多，提供更好的用户体验
  //     if (scrollHeight - scrollTop - clientHeight < 100) {
  //       loadMoreData();
  //     }
  //   }, 300), // 300ms的节流时间，避免短时间内多次触发
  //   [loadMoreData, loading, hasMore],
  // );

  // // 优化时间范围变更处理
  // const handleTimeRangeChange = useCallback(
  //   (range: [string, string] | undefined, preset?: string | undefined, displayText?: string) => {
  //     setTimeRange(range);
  //     setTimeRangePreset(preset);
  //     setTimeDisplayText(displayText);

  //     // 当有时间范围变化时，如果是有数据的情况下，重新加载数据
  //     if (range && tableDataRef.current.length > 0) {
  //       resetData();
  //     }
  //   },
  //   [resetData],
  // );

  // // 优化搜索提交处理
  // const handleSubmitSearch = useCallback(() => {
  //   // 重置数据，触发新查询
  //   if (tableDataRef.current.length > 0) {
  //     resetData();
  //   }
  // }, [resetData]);

  // // 优化SQL查询提交处理
  // const handleSubmitSql = useCallback(() => {
  //   // 重置数据，触发新查询
  //   if (tableDataRef.current.length > 0) {
  //     resetData();
  //   }
  // }, [resetData]);

  // 优化字段选择组件的props
  const fieldSelectorProps = useMemo(
    () => ({
      // selectedTable,
      // availableFields,
      // selectedFields,
      // onToggleField: toggleFieldSelection,
      // lastAddedField: lastAddedFieldRef.current,
      // lastRemovedField: lastRemovedFieldRef.current,
      // availableTables: availableTablesRef.current,
      // onTableChange: handleTableChange,
      // collapsed,
      // loading: fieldLoading,
      moduleNames,
      fetchModuleNames,
    }),
    [
      // selectedTable,
      // availableFields,
      // selectedFields,
      // toggleFieldSelection,
      // collapsed,
      // renderKey,
      // fieldLoading,
      // handleTableChange,
      moduleNames,
      fetchModuleNames,
    ],
  );

  // 优化DataTable组件的props
  const dataTableProps = useMemo(
    () => ({
      log,
      // data: tableData,
      fetchLog,
      // hasMore,
      // selectedFields,
      // searchQuery,
      // viewMode,
      // onScroll: handleScroll,
      // lastAddedField: lastAddedFieldRef.current,
    }),
    // [tableData, loading, hasMore, selectedFields, searchQuery, viewMode, handleScroll, renderKey],
    [log, fetchLog],
  );

  // 优化视图模式切换的处理函数
  // const handleViewModeChange = useCallback((mode: 'table' | 'json') => {
  //   setViewMode(mode);
  // }, []);

  // // 优化直方图显示切换的处理函数
  // const handleToggleHistogram = useCallback((show: boolean) => {
  //   setShowHistogram(show);
  // }, []);

  // useEffect(() => {
  //   // 添加调试代码，检查HomePage中的直方图显示条件
  //   console.log('HomePage 直方图显示条件:', {
  //     showHistogram,
  //     hasDistributionData: distributionData && distributionData.length > 0,
  //     distributionDataLength: distributionData ? distributionData.length : 0,
  //   });
  // }, [showHistogram, distributionData]);

  const [siderWidth, setSiderWidth] = useState(200);
  const [isDragging, setIsDragging] = useState(false);
  const dragStartX = useRef(0);
  const dragStartWidth = useRef(0);

  const handleDragStart = (e: React.MouseEvent) => {
    setIsDragging(true);
    dragStartX.current = e.clientX;
    dragStartWidth.current = siderWidth;
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
  };

  const handleDragMove = useCallback(
    (e: MouseEvent) => {
      if (!isDragging) return;

      const deltaX = e.clientX - dragStartX.current;
      const newWidth = Math.max(200, Math.min(600, dragStartWidth.current + deltaX));
      setSiderWidth(newWidth);
    },
    [isDragging],
  );

  const handleDragEnd = useCallback(() => {
    setIsDragging(false);
    document.body.style.cursor = '';
    document.body.style.userSelect = '';
  }, []);

  useEffect(() => {
    if (isDragging) {
      document.addEventListener('mousemove', handleDragMove);
      document.addEventListener('mouseup', handleDragEnd);
    }
    return () => {
      document.removeEventListener('mousemove', handleDragMove);
      document.removeEventListener('mouseup', handleDragEnd);
    };
  }, [isDragging, handleDragMove, handleDragEnd]);

  return (
    <div className={styles.layout}>
      {/* 搜索 */}
      <SearchBar
        // searchQuery={searchQuery}
        totalCount={log?.totalCount}
        onSearch={onSearch}
        // whereSql={whereSql}
        // timeRange={timeRange}
        // timeRangePreset={timeRangePreset}
        // timeDisplayText={timeDisplayText}
        // timeGrouping={timeGrouping}
        // onWhereSqlChange={setWhereSql}
        // onSubmitSearch={handleSubmitSearch}
        // onSubmitSql={handleSubmitSql}
        // onTimeRangeChange={handleTimeRangeChange}
        // onOpenTimeSelector={() => setShowTimePicker(true)}
        // onTimeGroupingChange={setTimeGrouping}
      />

      <div className={styles.container}>
        {/* 模块 */}
        <div className={styles.left} style={{ width: siderWidth }}>
          <SiderComponent {...fieldSelectorProps} />
          <div className={styles.dragHandle} onMouseDown={handleDragStart} />
        </div>

        {/* 内容 */}
        <div className={styles.right}>
          <Log {...dataTableProps} />
        </div>
      </div>
    </div>
  );
};

// 确保使用命名导出和默认导出两种方式
export { HomePage };
export default HomePage;
