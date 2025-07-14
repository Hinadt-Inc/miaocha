import { useState, useEffect, useMemo } from 'react';
import { Splitter } from 'antd';
import HistogramChart from './HistogramChart';
import styles from './Log.module.less';
import VirtualTable from './VirtualTable';
interface IProps {
  histogramData: ILogHistogramData[]; // 直方图数据
  histogramDataLoading: boolean; // 直方图数据是否正在加载
  getDetailData: any; // 加载日志数据的函数
  detailData: ILogDetailsResponse; // 日志数据;
  searchParams: ILogSearchParams; // 搜索参数
  dynamicColumns?: ILogColumnsResponse[]; // 添加动态列配置
  whereSqlsFromSider: IStatus[]; // 侧边栏的where条件
  sqls?: string[]; // SQL语句列表
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
  onChangeColumns: (params: ILogColumnsResponse[]) => void; // 列变化回调函数
  onSearchFromTable?: (params: ILogSearchParams) => void; // 来自表格的搜索回调
  moduleQueryConfig?: any; // 模块查询配置
  onSortChange?: (sortConfig: any[]) => void; // 排序变化回调函数
}

const Log = (props: IProps) => {
  const {
    histogramData,
    detailData,
    getDetailData,
    dynamicColumns = [],
    searchParams,
    whereSqlsFromSider,
    sqls,
    onSearch,
    onChangeColumns,
    onSearchFromTable,
    moduleQueryConfig,
    onSortChange,
  } = props || {};
  const { rows = [], totalCount } = detailData || {};
  const [allRows, setAllRows] = useState<any[]>([]); // 用于存储所有历史数据的状态

  // 当新数据到达时，将其添加到历史数据中
  useEffect(() => {
    const { offset } = getDetailData.params?.[0] || {};
    setAllRows((prevRows) => {
      if (offset === 0) {
        // 首次加载或刷新，直接替换
        if (prevRows.length === rows.length && prevRows[0]?._key === rows[0]?._key) {
          return prevRows;
        }
        return rows.length > 0 ? rows : [];
      } else if (rows.length > 0) {
        // 加载更多，避免重复拼接
        const lastKey = prevRows[prevRows.length - 1]?._key;
        const firstNewKey = rows[0]?._key;
        if (lastKey === firstNewKey) {
          // 已经拼接过，不再拼接
          return prevRows;
        }
        return [...prevRows, ...rows];
      } else {
        return prevRows;
      }
    });
  }, [rows]);

  const handleLoadMore = () => {
    if (!getDetailData.loading) {
      getDetailData.run({
        ...getDetailData.params?.[0],
        offset: allRows.length || 0,
      });
    }
  };

  const tableProps = useMemo(() => {
    return {
      whereSqlsFromSider,
      onChangeColumns,
      data: allRows,
      searchParams,
      loading: getDetailData?.loading,
      onLoadMore: handleLoadMore,
      hasMore: totalCount ? allRows.length < totalCount : false,
      dynamicColumns,
      sqls,
      onSearch: onSearchFromTable,
      moduleQueryConfig,
      onSortChange,
    };
  }, [
    allRows,
    getDetailData?.loading,
    totalCount,
    handleLoadMore,
    dynamicColumns,
    searchParams,
    whereSqlsFromSider,
    sqls,
    onSearchFromTable,
    onSortChange,
  ]);

  return (
    <Splitter layout="vertical" className={styles.logContainer}>
      <Splitter.Panel collapsible defaultSize={170} min={170} max={170}>
        <div className={styles.chart}>
          <HistogramChart data={histogramData as any} searchParams={searchParams} onSearch={onSearch} />
        </div>
      </Splitter.Panel>
      <Splitter.Panel>
        <div className={styles.table}>
          <VirtualTable {...tableProps} />
        </div>
      </Splitter.Panel>
    </Splitter>
  );
};

export default Log;
