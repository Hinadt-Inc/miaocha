/**
 * Log 模块主组件
 * 重构后的模块化版本，提供更好的代码组织和可维护性
 */

import React from 'react';

import { Splitter } from 'antd';

import { LogChart } from './components';
// import { useLogData, useTableProps } from './hooks';
import styles from './styles/Log.module.less';

const Log: React.FC = () => {
  // // 使用自定义 Hook 管理数据状态
  // const { allRows, totalCount, loading, handleLoadMore, hasMore } = useLogData(getDetailData, detailData);

  // // 使用自定义 Hook 生成表格属性
  // const tableProps = useTableProps(allRows, loading, totalCount, handleLoadMore, hasMore, {
  //   whereSqlsFromSider,
  //   onChangeColumns,
  //   searchParams,
  //   dynamicColumns,
  //   sqls,
  //   onSearch: onSearchFromTable,
  //   moduleQueryConfig,
  //   onSortChange,
  // });

  return (
    <Splitter className={styles.logContainer} layout="vertical">
      <Splitter.Panel collapsible defaultSize={170} max={170} min={170}>
        <LogChart />
      </Splitter.Panel>
      <Splitter.Panel>{/* <LogTable {...tableProps} /> */}</Splitter.Panel>
    </Splitter>
  );
};

export default Log;
