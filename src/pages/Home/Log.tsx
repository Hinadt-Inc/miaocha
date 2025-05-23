import { useState, useEffect, useMemo } from 'react';
import { Spin } from 'antd';
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
}

const Log = (props: IProps) => {
  const {
    histogramData,
    histogramDataLoading,
    detailData,
    getDetailData,
    dynamicColumns = [],
    searchParams,
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
        if (prevRows.length === rows.length) return prevRows;
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
      data: allRows,
      searchParams,
      loading: getDetailData?.loading,
      onLoadMore: handleLoadMore,
      hasMore: totalCount ? allRows.length < totalCount : false,
      dynamicColumns,
    }),
    [allRows, getDetailData?.loading, totalCount, handleLoadMore, dynamicColumns, searchParams],
  );

  return (
    <div className={styles.logContainer}>
      <div className={styles.chart}>
        <Spin size="small" spinning={histogramDataLoading}>
          <HistogramChart data={histogramData} searchParams={searchParams} />
        </Spin>
      </div>
      <div className={styles.table}>
        <VirtualTable {...tableProps} />
      </div>
    </div>
  );
};

export default Log;
