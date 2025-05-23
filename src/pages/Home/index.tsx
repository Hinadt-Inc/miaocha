import { useState, useEffect, useMemo, useRef } from 'react';
import { Splitter } from 'antd';
import { useRequest } from 'ahooks';
import * as api from '@/api/logs';
import SearchBar from './SearchBar';
import Log from './Log';
import Sider from './Sider';
import { QUICK_RANGES, DATE_FORMAT } from './utils';
import styles from './index.module.less';

const HomePage = () => {
  const [moduleOptions, setModuleOptions] = useState<IStatus[]>([]); // 模块名称列表，用于字段选择等组件
  const [detailData, setDetailData] = useState<ILogDetailsResponse | null>(null); // 日志数据
  const [logTableColumns, setLogTableColumns] = useState<ILogColumnsResponse[]>([]); // 日志字段列表
  const [histogramData, setHistogramData] = useState<ILogHistogramData[] | null>(null); // 日志时间分布列表
  const searchBarRef = useRef<HTMLDivElement>(null);

  // 默认的搜索参数
  const defaultSearchParams: ILogSearchParams = {
    offset: 0,
    pageSize: 20,
    datasourceId: null,
    module: null,
    startTime: QUICK_RANGES.last_15m.from().format(DATE_FORMAT),
    endTime: QUICK_RANGES.last_15m.to().format(DATE_FORMAT),
    timeRange: 'last_15m',
    timeGrouping: 'auto',
  };
  // 日志检索请求参数
  const [searchParams, setSearchParams] = useState<ILogSearchParams>(defaultSearchParams);

  // 获取模块名称
  const generateModuleHierarchy = (modulesData: IMyModulesResponse[]): IStatus[] => {
    return (
      modulesData?.map(({ modules, datasourceId, datasourceName }) => ({
        label: String(datasourceName),
        value: String(datasourceId),
        children:
          modules?.map(({ moduleName }) => ({
            value: String(moduleName),
            label: String(moduleName),
          })) || [],
      })) || []
    );
  };

  // 获取模块列表
  const getMyModules = useRequest(api.fetchMyModules, {
    onSuccess: (res) => {
      const moduleHierarchy = generateModuleHierarchy(res);

      // 设置默认数据源和模块
      if ((!searchParams.datasourceId || !searchParams.module) && moduleHierarchy[0]) {
        setSearchParams((prev) => ({
          ...prev,
          datasourceId: Number(moduleHierarchy[0].value),
          module: moduleHierarchy[0]?.children[0]?.value,
        }));
      } else {
        // todo 当切换时，需要更新数据源和模块，并更新日志数据
      }
      setModuleOptions(moduleHierarchy);
    },
  });

  // 执行日志明细查询
  const getDetailData = useRequest(api.fetchLogDetails, {
    manual: true,
    onSuccess: (res) => {
      const { rows } = res;
      // 为每条记录添加唯一ID
      (rows || []).map((item, index) => {
        item._key = `${Date.now()}_${index}`;
      });
      setDetailData(res);
    },
    onError: () => {
      setDetailData(null);
    },
  });

  // 执行日志时间分布查询
  const getHistogramData = useRequest(api.fetchLogHistogram, {
    manual: true,
    onSuccess: (res) => {
      setHistogramData(res?.distributionData || []);
    },
    onError: () => {
      setHistogramData(null);
    },
  });

  useEffect(() => {
    // 只判断 datasourceId和module，因为这是查询的必要参数
    if (searchParams.datasourceId && searchParams.module) {
      getDetailData.run(searchParams);
      getHistogramData.run(searchParams);
    }
  }, [searchParams]);

  // 处理列变化
  const handleChangeColumns = (columns: ILogColumnsResponse[]) => {
    setLogTableColumns(columns);
  };

  // 优化字段选择组件的props
  const siderProps = {
    searchParams,
    modules: moduleOptions,
    moduleLoading: getMyModules.loading,
    detailLoading: getDetailData.loading,
    onSearch: setSearchParams,
    onChangeColumns: handleChangeColumns,
    onChangeSql: (sql: string) => (searchBarRef?.current as any)?.renderSql?.(sql),
  };

  // 优化log组件的props
  const logProps: any = useMemo(
    () => ({
      histogramData,
      histogramDataLoading: getHistogramData.loading,
      detailData,
      getDetailData,
      searchParams,
      dynamicColumns: logTableColumns,
    }),
    [histogramData, getHistogramData.loading, detailData, getDetailData, logTableColumns, searchParams],
  );

  // 搜索栏组件props
  const searchBarProps = useMemo(
    () => ({
      searchParams,
      totalCount: detailData?.totalCount,
      loading: getDetailData?.loading || getHistogramData.loading,
      onSubmit: setSearchParams,
    }),
    [searchParams, detailData?.totalCount, getDetailData?.loading, getHistogramData.loading, setSearchParams],
  );

  return (
    <div className={styles.layout}>
      <SearchBar ref={searchBarRef} {...searchBarProps} />

      <Splitter className={styles.container}>
        <Splitter.Panel collapsible defaultSize={260} min={260} max="70%">
          <Sider {...siderProps} />
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
