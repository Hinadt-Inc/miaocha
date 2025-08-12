/**
 * Log 模块主组件
 * 重构后的模块化版本，提供更好的代码组织和可维护性
 */

import React from 'react';
import { Splitter } from 'antd';
import { LogChart, LogTable } from './components';
import { useLogData, useTableProps } from './hooks';
import { ILogProps } from './types';
import styles from './styles/Log.module.less';

const Log: React.FC<ILogProps> = (props) => {
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
  } = props;

  // 使用自定义 Hook 管理数据状态
  const { allRows, totalCount, loading, handleLoadMore, hasMore } = useLogData(getDetailData, detailData);

  // 使用自定义 Hook 生成表格属性
  const tableProps = useTableProps(allRows, loading, totalCount, handleLoadMore, hasMore, {
    whereSqlsFromSider,
    onChangeColumns,
    searchParams,
    dynamicColumns,
    sqls,
    onSearch: onSearchFromTable,
    moduleQueryConfig,
    onSortChange,
  });

  return (
    <Splitter layout="vertical" className={styles.logContainer}>
      <Splitter.Panel collapsible defaultSize={170} min={170} max={170}>
        <LogChart data={histogramData} searchParams={searchParams} onSearch={onSearch} />
      </Splitter.Panel>
      <Splitter.Panel>
        <LogTable {...tableProps} />
      </Splitter.Panel>
    </Splitter>
  );
};

export default Log;
