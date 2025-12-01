import { useMemo } from 'react';

import { Table, Tabs } from 'antd';
import ReactJson from 'react-json-view';

import { highlightText } from '@/utils/highlightText';

import { useHomeContext } from '../context';

import { REACT_JSON_CONFIG, TAB_KEYS } from './constants';
import styles from './index.module.less';
import type { IExpandedRowProps } from './types';
import { formatFieldValue } from './utils';

/**
 * 展开行组件
 * 用于在表格中展示详细的日志数据，支持表格和JSON两种展示方式
 */
const ExpandedRow: React.FC<IExpandedRowProps> = (props) => {
  const { data, keywords, enhancedColumns } = props;
  const { moduleQueryConfig, searchParams } = useHomeContext();

  // const hasSource = useMemo(() => enhancedColumns.some((item) => item.dataIndex === '_source'), [enhancedColumns]);
  const showKey = useMemo(() => {
    return searchParams.fields || [];
    // const temp: string[] = enhancedColumns.map((item) => item.dataIndex);
    // if (hasSource) {
    //   return searchParams.fields || [];
    // }
    // return temp;
  }, [enhancedColumns]);

  // 使用hooks生成表格配置和数据
  const columns = useMemo(
    () => [
      {
        title: '字段',
        className: 'field-title',
        dataIndex: 'field',
        width: 150,
      },
      {
        title: '值',
        dataIndex: 'value',
        render: (text: string) => highlightText(text, keywords),
      },
    ],
    [keywords],
  );

  const dataSource = useMemo(() => {
    const excludeFields = moduleQueryConfig?.excludeFields || [];
    const result: { key: string; field: string; value: any }[] = [];
    Object.entries(data).forEach(([key, value], index) => {
      if (!excludeFields.includes(key)) {
        if ([...showKey].includes(key)) {
          result.push({
            key: `${key}_${index}`,
            field: key,
            value: formatFieldValue(key, value, moduleQueryConfig?.timeField as string),
          });
        }
      }
    });
    return result;
  }, [data, moduleQueryConfig]);

  const filteredJsonData = useMemo(() => {
    const filteredData: Record<string, any> = {};
    const excludeFields = moduleQueryConfig?.excludeFields || [];
    Object.entries(data).forEach(([key, value]) => {
      if (!excludeFields.includes(key)) {
        // const showKeyList: string[] = hasSource ? showKey : [...showKey, '_originalSource'];
        if ([...showKey].includes(key)) {
          filteredData[key] = value;
        }
      }
    });
    return filteredData;
  }, [data, moduleQueryConfig]);

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
  const items = useMemo(
    () => [
      {
        key: 'Table',
        label: 'Table',
        children: tableComponent,
      },
      {
        key: 'JSON',
        label: 'JSON',
        children: jsonComponent,
      },
    ],
    [tableComponent, jsonComponent],
  );

  return <Tabs className={styles.expandedRow} defaultActiveKey={TAB_KEYS.TABLE} items={items} size="small" />;
};

export default ExpandedRow;
