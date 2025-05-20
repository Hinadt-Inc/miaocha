import { useState, useEffect, useMemo } from 'react';
import { Splitter } from 'antd';
import { useRequest } from 'ahooks';
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
        item._key = `${Date.now()}_${index}`;
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
    setLog(null);
  };

  // 使用useMemo优化搜索参数构建，减少不必要的对象创建
  // const [timeGrouping, setTimeGrouping] = useState<'minute' | 'hour' | 'day' | 'month'>('minute');

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
      searchParams,
      dynamicColumns: logColumns,
    }),
    [log, fetchLog, logColumns, searchParams],
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
