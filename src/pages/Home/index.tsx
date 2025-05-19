import { useState, useEffect, useMemo } from 'react';
import { Splitter } from 'antd';
import { useRequest } from 'ahooks';
<<<<<<< HEAD
=======
import { useFilters } from '@/hooks/useFilters';
>>>>>>> feature/zhangyongjian/除发现部分的其他修改
import SearchBar from './SearchBar';
import Log from './Log';
import Sider from './Sider';
import * as api from '@/api/permission';
import { searchLogs } from '@/api/logs';
import styles from './index.module.less';

const HomePage = () => {
  const [moduleNames, setModuleNames] = useState<IStatus[]>([]); // 模块名称列表，用于字段选择等组件
  const [log, setLog] = useState<ISearchLogsResponse | null>(null); // 日志数据
  const [logColumns, setLogColumns] = useState<ILogColumnsResponse[]>([]); // 日志字段列表
  // 搜索参数
  const [searchParams, setSearchParams] = useState<ISearchLogsParams>({
    pageSize: 20,
    datasourceId: undefined,
    module: undefined,
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

  // 搜索
  const onSearch = (params: ISearchLogsParams) => {
    setSearchParams({
      datasourceId: searchParams.datasourceId,
      module: searchParams.module,
      pageSize: searchParams.pageSize,
      startTime: searchParams.startTime,
      endTime: searchParams.endTime,
      ...params,
    });
  };

  // 使用useMemo优化搜索参数构建，减少不必要的对象创建
<<<<<<< HEAD
  // const [timeGrouping, setTimeGrouping] = useState<'minute' | 'hour' | 'day' | 'month'>('minute');
=======
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
>>>>>>> feature/zhangyongjian/除发现部分的其他修改

  // 处理列变化
  const handleColumnsChange = (columns: ILogColumnsResponse[]) => {
    setLogColumns(columns);
  };

  // 优化字段选择组件的props
  const siderProps = useMemo(
    () => ({
      moduleNames,
      moduleLoading: fetchModuleNames.loading,
      logLoading: fetchLog.loading,
      fieldDistributions: log?.fieldDistributions,
      onColumnsChange: handleColumnsChange,
    }),
    [log?.fieldDistributions, fetchLog.loading, moduleNames, fetchModuleNames.loading],
  );

  // 优化DataTable组件的props
  const dataTableProps = useMemo(
    () => ({
      log,
      fetchLog,
      logColumns,
    }),
    [log, fetchLog, logColumns],
  );

  // 搜索栏组件props
  const searchBarProps = useMemo(
    () => ({
      onSearch,
      totalCount: log?.totalCount,
      loading: fetchLog?.loading,
    }),
    [fetchLog.loading, onSearch, log?.totalCount],
  );

  return (
    <div className={styles.layout}>
      <SearchBar {...searchBarProps} />

      <Splitter className={styles.container}>
        <Splitter.Panel collapsible defaultSize={260} min={260} max="70%">
          <Sider {...(siderProps as any)} />
        </Splitter.Panel>
        <Splitter.Panel collapsible>
          <div className={styles.right}>
            <Log {...(dataTableProps as any)} />
          </div>
        </Splitter.Panel>
      </Splitter>
    </div>
  );
};

export default HomePage;
