import { Suspense, lazy, useState, useEffect, useMemo } from 'react';
import SpinIndicator from '@/components/SpinIndicator';
import styles from './Log.module.less';
import VirtualTable from './VirtualTable';

// 使用懒加载优化初始加载时间
const HistogramChart = lazy(() => import('./HistogramChart'));

interface IProps {
  fetchLog: any; // 加载日志数据的函数
  log: {
    totalCount?: number; // 总行数
    distributionData?: any; // 直方图数据
    rows?: any[]; // 表格数据
  };
  dynamicColumns?: ILogColumnsResponse[]; // 添加动态列配置
}

const Log = (props: IProps) => {
  const { log, fetchLog, dynamicColumns = [] } = props;
  const { rows } = log || {};
  const [allRows, setAllRows] = useState<any[]>([]); // 用于存储所有历史数据的状态
  // 当新数据到达时，将其添加到历史数据中
  useEffect(() => {
    if (rows && rows.length > 0) {
      setAllRows((prevRows: any) => [...prevRows, ...rows]);
    } else if (!log?.totalCount && (rows || [])?.length === 0) {
      setAllRows([]);
    }
  }, [rows, log]);

  const handleLoadMore = () => {
    if (!fetchLog.loading) {
      fetchLog.run({
        ...fetchLog.params?.[0],
        offset: allRows.length || 0,
      });
    }
  };

  const tableProps = useMemo(
    () => ({
      data: allRows,
      loading: fetchLog.loading,
      onLoadMore: handleLoadMore,
      hasMore: log?.totalCount ? allRows.length < log.totalCount : false,
      dynamicColumns,
    }),
    [allRows, fetchLog?.loading, log?.totalCount, handleLoadMore, dynamicColumns],
  );
  return (
    <div className={styles.logContainer}>
      <div className={styles.chart}>
        <Suspense fallback={<SpinIndicator />}>
          <HistogramChart data={log?.distributionData} />
        </Suspense>
      </div>
      <div className={styles.table}>
        <VirtualTable {...tableProps} />
      </div>
    </div>
  );
};

export default Log;
