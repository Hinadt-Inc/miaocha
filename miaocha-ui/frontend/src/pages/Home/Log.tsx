import { useState, useEffect, useMemo } from 'react';
import { Splitter } from 'antd';
import HistogramChart from './HistogramChart';
import styles from './Log.module.less';
import VirtualTable from './VirtualTable';
interface IProps {
  histogramData: ILogHistogramResponse | null; // 直方图数据
  histogramDataLoading: boolean; // 直方图数据是否正在加载
  getDetailData: any; // 加载日志数据的函数
  detailData: ILogDetailsResponse; // 日志数据;
  searchParams: ILogSearchParams; // 搜索参数
  dynamicColumns?: ILogColumnsResponse[]; // 添加动态列配置
  whereSqlsFromSider: IStatus[]; // 侧边栏的where条件
  sqls?: string[]; // SQL语句列表
  onSearch: (params: ILogSearchParams) => void; // 搜索回调函数
  onChangeColumns: (col: any) => void; // 列变化回调函数 - 传递单个列对象
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

  const histogramChartData: ILogHistogramData | null = (() => {
    console.log('📊 开始处理histogramData:', histogramData);

    if (!histogramData) {
      console.log('📊 histogramData为空');
      return null;
    }

    // 检查数据结构 - 如果是ILogHistogramResponse格式
    if (histogramData.distributionData && Array.isArray(histogramData.distributionData)) {
      console.log('📊 histogramData.distributionData是数组, 长度:', histogramData.distributionData.length);

      if (histogramData.distributionData.length === 0) {
        console.log('📊 distributionData是空数组');
        return null;
      }

      const firstItem = histogramData.distributionData[0];
      console.log('📊 第一个元素:', firstItem);

      // 确保返回的是ILogHistogramData类型
      if (firstItem?.distributionData && firstItem?.timeUnit && firstItem?.timeInterval) {
        return firstItem;
      }
    }

    // 检查是否histogramData本身就是ILogHistogramData格式
    const directData = histogramData as unknown as ILogHistogramData;
    if (
      directData?.distributionData &&
      Array.isArray(directData.distributionData) &&
      directData?.timeUnit &&
      directData?.timeInterval
    ) {
      console.log('📊 histogramData本身就是ILogHistogramData格式');
      return directData;
    }

    console.log('📊 无法识别的数据格式或数据为空');
    return null;
  })();

  // 调试日志
  console.log('📊 Log组件接收到的histogramData:', histogramData);
  console.log('📊 histogramData的结构检查:', {
    hasHistogramData: !!histogramData,
    hasDistributionData: !!histogramData?.distributionData,
    distributionDataLength: histogramData?.distributionData?.length,
    distributionDataType: typeof histogramData?.distributionData,
    isDistributionDataArray: Array.isArray(histogramData?.distributionData),
    firstDistributionItem: histogramData?.distributionData?.[0],
  });
  console.log('📊 传递给HistogramChart的data:', histogramChartData);

  return (
    <Splitter layout="vertical" className={styles.logContainer}>
      <Splitter.Panel collapsible defaultSize={170} min={170} max={170}>
        <div className={styles.chart}>
          <HistogramChart data={histogramChartData} searchParams={searchParams} onSearch={onSearch} />
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
