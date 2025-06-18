import { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { Splitter } from 'antd';
import { useRequest } from 'ahooks';
import * as api from '@/api/logs';
import SearchBar from './SearchBar';
import Log from './Log';
import Sider from './Sider';
import { QUICK_RANGES, DATE_FORMAT_THOUSOND } from './utils';
import styles from './index.module.less';
import dayjs from 'dayjs';

const HomePage = () => {
  const [moduleOptions, setModuleOptions] = useState<IStatus[]>([]); // 模块名称列表，用于字段选择等组件
  const [detailData, setDetailData] = useState<ILogDetailsResponse | null>(null); // 日志数据
  const [logTableColumns, setLogTableColumns] = useState<ILogColumnsResponse[]>([]); // 日志字段列表
  const [histogramData, setHistogramData] = useState<ILogHistogramData | null>(null); // 日志时间分布列表
  const [whereSqlsFromSider, setWhereSqlsFromSider] = useState<IStatus[]>([]); // 侧边栏的where条件
  const [sqls, setSqls] = useState<string[]>([]); // SQL语句列表
  const [activeColumns, setActiveColumns] = useState<string[]>([]); // 激活的字段列表
  const searchBarRef = useRef<any>(null);
  const siderRef = useRef<any>(null);
  const abortRef = useRef<AbortController | null>(null);

  // 默认的搜索参数
  const defaultSearchParams: ILogSearchParams = {
    offset: 0,
    pageSize: 20,
    datasourceId: null,
    module: null,
    startTime: QUICK_RANGES.last_15m.from().format(DATE_FORMAT_THOUSOND),
    endTime: QUICK_RANGES.last_15m.to().format(DATE_FORMAT_THOUSOND),
    timeRange: 'last_15m',
    timeGrouping: 'auto',
  };
  // 日志检索请求参数
  const [searchParams, setSearchParams] = useState<ILogSearchParams>(defaultSearchParams);

  // 获取模块选项
  const generateModuleOptions = (modulesData: IMyModulesResponse[]): IStatus[] => {
    return (
      modulesData?.map(({ datasourceId, datasourceName, module }) => ({
        label: module,
        value: module,
        datasourceId,
        datasourceName,
        module,
      })) || []
    );
  };

  // 获取模块列表
  useRequest(api.fetchMyModules, {
    onSuccess: (res) => {
      const moduleOptions = generateModuleOptions(res);
      // 设置默认数据源和模块
      if ((!searchParams.datasourceId || !searchParams.module) && moduleOptions[0]) {
        setSearchParams((prev) => ({
          ...prev,
          datasourceId: Number(moduleOptions[0].datasourceId),
          module: localStorage.getItem('favoriteModule') || moduleOptions[0].module,
        }));
      }
      setModuleOptions(moduleOptions);
    },
  });

  // 执行日志明细查询
  const getDetailData = useRequest(
    async (params: ILogSearchParams & { signal?: AbortSignal }) => {
      // 传 signal 给 api
      return api.fetchLogDetails(params, { signal: params.signal });
    },
    {
      manual: true,
      onSuccess: (res) => {
        const { rows } = res;
        // 为每条记录添加唯一ID
        (rows || []).map((item, index) => {
          item._key = `${Date.now()}_${index}`;
          item['log_time'] = item['log_time'] ? dayjs(item['log_time'] as string).format(DATE_FORMAT_THOUSOND) : '';
        });
        setDetailData(res);
      },
      onError: () => {
        setDetailData(null);
      },
    },
  );

  // 执行日志时间分布查询
  const getHistogramData = useRequest(
    async (params: ILogSearchParams & { signal?: AbortSignal }) => {
      // 传 signal 给 api
      return api.fetchLogHistogram(params, { signal: params.signal });
    },
    {
      manual: true,
      onSuccess: (res: any) => {
        setHistogramData(res);
      },
      onError: () => {
        setHistogramData(null);
      },
    },
  );

  useEffect(() => {
    // 只判断 datasourceId和module，因为这是查询的必要参数
    if (searchParams.datasourceId && searchParams.module) {
      if (abortRef.current) abortRef.current.abort(); // 取消上一次
      abortRef.current = new AbortController();
      getDetailData.run({ ...searchParams, signal: abortRef.current.signal });
      getHistogramData.run({ ...searchParams, signal: abortRef.current.signal });
    }
  }, [searchParams]);

  // 处理列变化
  const handleChangeColumns = (columns: ILogColumnsResponse[]) => {
    setLogTableColumns(columns);
  };

  const handleSetWhereSqlsFromSider = (flag: '=' | '!=', columnName: string, value: string) => {
    const sql = `${columnName} ${flag} '${value}'`;
    const newSearchParams = {
      ...searchParams,
      offset: 0,
    };
    // 添加where条件
    if (flag === '=') {
      const oldSql = `${columnName} != '${value}'`;
      newSearchParams.whereSqls = [...(searchParams?.whereSqls || []), sql];
      (searchBarRef?.current as any)?.removeSql?.(oldSql);
      setWhereSqlsFromSider((prev: any) => [
        ...prev,
        {
          label: sql,
          value: value,
          field: columnName,
        },
      ]);
    } else {
      // 删除where条件
      const oldSql = `${columnName} = '${value}'`;
      newSearchParams.whereSqls = searchParams?.whereSqls?.filter((item: any) => item !== oldSql);
      (searchBarRef?.current as any)?.removeSql?.(oldSql);
      setWhereSqlsFromSider((prev: any) => prev.filter((item: any) => item.value !== value));
    }
    // 如果where条件为空，则删除where条件
    if (newSearchParams?.whereSqls?.length === 0) {
      delete newSearchParams.whereSqls;
    }
    // 添加sql
    (searchBarRef?.current as any)?.addSql?.(sql);
    setSearchParams(newSearchParams);
  };

  // 优化字段选择组件的props
  const siderProps = {
    searchParams,
    modules: moduleOptions,
    setWhereSqlsFromSider: handleSetWhereSqlsFromSider,
    onSearch: setSearchParams,
    onChangeColumns: handleChangeColumns,
    onActiveColumnsChange: setActiveColumns,
  };

  // 使用useCallback稳定getDistributionWithSearchBar函数引用
  const getDistributionWithSearchBar = useCallback(() => {
    siderRef.current?.getDistributionWithSearchBar?.();
  }, []);

  const onSearchFromLog = (params: ILogSearchParams) => {
    // 绝对时间
    //   {
    //     "label": "2025-05-06 00:07:00 ~ 2025-05-23 05:04:26",
    //     "value": "2025-05-06 00:07:00 ~ 2025-05-23 05:04:26",
    //     "range": [
    //         "2025-05-06 00:07:00",
    //         "2025-05-23 05:04:26"
    //     ]
    // }
    const { startTime, endTime } = params;
    const timeOption = {
      label: `${startTime} ~ ${endTime}`,
      value: `${startTime} ~ ${endTime}`,
      range: [startTime, endTime],
      type: 'absolute',
    };
    (searchBarRef?.current as any)?.setTimeOption(timeOption);
  };

  const handleChangeColumnsByLog = (col: any) => {
    const index = logTableColumns.findIndex((item) => item.columnName === col.title);
    if (index === -1) return;
    logTableColumns[index].selected = false;
    delete logTableColumns[index]._createTime;
    setLogTableColumns([...logTableColumns]);
  };

  // 优化log组件的props
  const logProps: any = useMemo(
    () => ({
      histogramData,
      detailData,
      getDetailData,
      searchParams,
      dynamicColumns: logTableColumns,
      whereSqlsFromSider,
      sqls,
      onSearch: onSearchFromLog,
      onChangeColumns: handleChangeColumnsByLog,
    }),
    [histogramData, detailData, getDetailData, logTableColumns, searchParams, whereSqlsFromSider, sqls],
  );

  // 搜索栏组件props
  const searchBarProps = useMemo(
    () => ({
      searchParams,
      totalCount: detailData?.totalCount,
      onSearch: setSearchParams,
      setWhereSqlsFromSider,
      columns: logTableColumns,
      onSqlsChange: setSqls,
      activeColumns,
      getDistributionWithSearchBar,
    }),
    [
      searchParams,
      detailData?.totalCount,
      setSearchParams,
      setWhereSqlsFromSider,
      logTableColumns,
      activeColumns,
      getDistributionWithSearchBar,
    ],
  );

  return (
    <div className={styles.layout}>
      <SearchBar ref={searchBarRef} {...searchBarProps} />

      <Splitter className={styles.container}>
        <Splitter.Panel collapsible defaultSize={200} min={0} max="40%">
          <Sider ref={siderRef} {...siderProps} />
        </Splitter.Panel>
        <Splitter.Panel collapsible>
          <div className={styles.right}>
            <Log {...logProps} />
          </div>
        </Splitter.Panel>
      </Splitter>
    </div>
  );
};

export default HomePage;
