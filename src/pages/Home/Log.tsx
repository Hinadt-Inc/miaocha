import { useState, useEffect, useMemo } from 'react';
import { Spin, Splitter } from 'antd';
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
  whereSqlsFromSider: string[]; //
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
  onChangeColumns: (params: ILogColumnsResponse[]) => void; // 列变化回调函数
}

const Log = (props: IProps) => {
  const {
    histogramData,
    histogramDataLoading,
    detailData,
    getDetailData,
    dynamicColumns = [],
    searchParams,
    whereSqlsFromSider,
    onSearch,
    onChangeColumns,
  } = props || {};
  const { rows = [], totalCount } = detailData || {};
  const [allRows, setAllRows] = useState<any[]>([]); // 用于存储所有历史数据的状态

  // 当新数据到达时，将其添加到历史数据中
  useEffect(() => {
    const { offset } = getDetailData.params?.[0] || {};
    if (offset === 0) {
      if (rows.length > 0) {
        setAllRows(rows);
      } else {
        setAllRows([]);
      }
    } else if (rows.length > 0) {
      setAllRows((prevRows: any) => {
        return [...prevRows, ...rows];
      });
    } else {
      setAllRows([]);
    }
  }, [rows, totalCount]);

  const handleLoadMore = () => {
    if (!getDetailData.loading) {
      getDetailData.run({
        ...getDetailData.params?.[0],
        offset: allRows.length || 0,
      });
    }
  };

  const tableProps = useMemo(
    () => ({
      whereSqlsFromSider,
      onChangeColumns,
      data: allRows,
      searchParams,
      loading: getDetailData?.loading,
      onLoadMore: handleLoadMore,
      hasMore: totalCount ? allRows.length < totalCount : false,
      dynamicColumns,
    }),
    [allRows, getDetailData?.loading, totalCount, handleLoadMore, dynamicColumns, searchParams, whereSqlsFromSider],
  );

  return (
    <Splitter layout="vertical" className={styles.logContainer}>
      <Splitter.Panel collapsible defaultSize={170} min={170} max={170}>
        <div className={styles.chart}>
          <Spin size="small" spinning={histogramDataLoading}>
            <HistogramChart data={histogramData as any} searchParams={searchParams} onSearch={onSearch} />
          </Spin>
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
