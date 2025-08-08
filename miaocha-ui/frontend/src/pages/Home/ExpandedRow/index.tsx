import React from 'react';
import { Table, Tabs } from 'antd';
import ReactJson from 'react-json-view';
import type { IExpandedRowProps } from './types';
import { useTableColumns, useTableDataSource, useFilteredJsonData, useTabItems } from './hooks';
import { REACT_JSON_CONFIG, TAB_KEYS } from './constants';
import styles from './index.module.less';

/**
 * 展开行组件
 * 用于在表格中展示详细的日志数据，支持表格和JSON两种展示方式
 */
const ExpandedRow: React.FC<IExpandedRowProps> = (props) => {
  const { data, keywords, moduleQueryConfig } = props;

  // 使用hooks生成表格配置和数据
  const columns = useTableColumns(keywords);
  const dataSource = useTableDataSource(data, moduleQueryConfig);
  const filteredJsonData = useFilteredJsonData(data, moduleQueryConfig);

  // 表格组件
  const tableComponent = React.useMemo(() => (
    <Table
      bordered
      dataSource={dataSource}
      columns={columns}
      pagination={false}
      size="small"
      rowKey="key"
    />
  ), [dataSource, columns]);

  // JSON组件
  const jsonComponent = React.useMemo(() => (
    <ReactJson
      src={filteredJsonData}
      collapsed={REACT_JSON_CONFIG.COLLAPSED_LEVEL}
      enableClipboard={REACT_JSON_CONFIG.ENABLE_CLIPBOARD}
      displayDataTypes={REACT_JSON_CONFIG.DISPLAY_DATA_TYPES}
      name={REACT_JSON_CONFIG.SHOW_NAME}
    />
  ), [filteredJsonData]);

  // Tabs配置
  const items = useTabItems(tableComponent, jsonComponent);

  return (
    <Tabs
      className={styles.expandedRow}
      size="small"
      defaultActiveKey={TAB_KEYS.TABLE}
      items={items}
    />
  );
};

export default ExpandedRow;
