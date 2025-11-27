import { useMemo } from 'react';

import { Table, Tabs } from 'antd';
import ReactJson from 'react-json-view';

import { REACT_JSON_CONFIG, TAB_KEYS } from './constants';
import { useTableColumns, useTableDataSource, useFilteredJsonData, useTabItems } from './hooks';
import styles from './index.module.less';
import type { IExpandedRowProps } from './types';

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
  const tableComponent = useMemo(
    () => <Table bordered columns={columns} dataSource={dataSource} pagination={false} rowKey="key" size="small" />,
    [dataSource, columns],
  );

  // JSON组件
  const jsonComponent = useMemo(
    () => (
      <ReactJson
        collapsed={REACT_JSON_CONFIG.COLLAPSED_LEVEL}
        displayDataTypes={REACT_JSON_CONFIG.DISPLAY_DATA_TYPES}
        enableClipboard={REACT_JSON_CONFIG.ENABLE_CLIPBOARD}
        name={REACT_JSON_CONFIG.SHOW_NAME}
        src={filteredJsonData}
      />
    ),
    [filteredJsonData],
  );

  // Tabs配置
  const items = useTabItems(tableComponent, jsonComponent);

  return <Tabs className={styles.expandedRow} defaultActiveKey={TAB_KEYS.TABLE} items={items} size="small" />;
};

export default ExpandedRow;
