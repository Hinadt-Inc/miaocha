import { useState, useEffect } from 'react';
import styles from './Log.module.less';
import VirtualTable from './VirtualTable';

interface IProps {
  log: {
    totalCount?: number;
    rows?: any[];
  };
  fetchLog: any;
  logColumns?: { columnName: string; selected: boolean }[]; // 添加动态列配置
}

const Log = (props: IProps) => {
  const { log, fetchLog, logColumns = [] } = props;

  const { rows } = log || {};
  // 用于存储所有历史数据的状态
  const [allRows, setAllRows] = useState<any[]>([]);

  // 当新数据到达时，将其添加到历史数据中
  useEffect(() => {
    if (rows && rows.length > 0) {
      setAllRows((prevRows) => [...prevRows, ...rows]);
    }
  }, [rows]);

  const handleLoadMore = () => {
    if (!fetchLog.loading) {
      fetchLog.run({
        ...fetchLog.params?.[0],
        offset: allRows.length || 0,
      });
    }
  };

  return (
    <div className={styles.logContainer}>
      <div className={styles.table}>
        <VirtualTable
          data={allRows}
          loading={fetchLog.loading}
          onLoadMore={handleLoadMore}
          hasMore={true}
          dynamicColumns={logColumns}
        />
      </div>
    </div>
  );
};

export default Log;
