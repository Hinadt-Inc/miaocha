import { Table, Tabs } from 'antd';
import { useMemo } from 'react';
import ReactJson from 'react-json-view';
import styles from './ExpandedRow.module.less';
import { highlightText } from '@/utils/highlightText';

interface IProps {
  data: Record<string, any>;
  keywords: string[]; // 搜索参数
}

const ExpandedRow = (props: IProps) => {
  const { data, keywords } = props;
  const columns = [
    {
      title: '字段',
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
    console.log('data', Object.entries(data));
    return Object.entries(data)
      .filter((item) => item[0] !== '_key')
      .map(([key, value], index) => ({
        key: `${key}_${index}`,
        field: key,
        value: key === 'log_time' ? String(value ?? '').replace('T', ' ') : String(value ?? ''),
      }));
  }, [data]);

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
