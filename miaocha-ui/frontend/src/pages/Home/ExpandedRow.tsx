import { Table, Tabs } from 'antd';
import { useMemo } from 'react';
import ReactJson from 'react-json-view';
import styles from './ExpandedRow.module.less';
import { highlightText } from '@/utils/highlightText';

interface IProps {
  data: Record<string, any>;
  keywords: string[]; // 搜索参数
  moduleQueryConfig?: any; // 模块查询配置
}

const ExpandedRow = (props: IProps) => {
  const { data, keywords, moduleQueryConfig } = props;
  const columns = [
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
  ];

  const dataSource = useMemo(() => {
    const timeField = moduleQueryConfig?.timeField || 'log_time'; // 如果没有配置则回退到log_time
    return Object.entries(data)
      .filter((item) => item[0] !== '_key')
      .map(([key, value], index) => ({
        key: `${key}_${index}`,
        field: key,
        value: key === timeField ? String(value ?? '').replace('T', ' ') : String(value ?? ''),
      }));
  }, [data, moduleQueryConfig]);

  const items = [
    {
      key: 'Table',
      label: 'Table',
      children: (
        <Table bordered dataSource={dataSource} columns={columns} pagination={false} size="small" rowKey="key" />
      ),
    },
    {
      key: 'JSON',
      label: 'JSON',
      children: <ReactJson src={data} collapsed={2} enableClipboard={true} displayDataTypes={false} name={false} />,
    },
  ];

  return <Tabs className={styles.expandedRow} size="small" defaultActiveKey="table" items={items} />;
};

export default ExpandedRow;
