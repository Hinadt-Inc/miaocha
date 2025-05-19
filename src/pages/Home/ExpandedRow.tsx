import { Table, Tabs } from 'antd';
import { useMemo } from 'react';
import ReactJson from 'react-json-view';
import styles from './ExpandedRow.module.less';

interface IProps {
  data: Record<string, any>;
}

const ExpandedRow = (props: IProps) => {
  const { data } = props;
  console.log('【打印日志】data22:', data);
  const columns = [
    {
      title: '字段',
      dataIndex: 'field',
      width: 150,
    },
    {
      title: '值',
      dataIndex: 'value',
      render: (_text: string, record: any) => {
        // message字段或内容较长时自动换行
        if (record.field === 'message' || String(record.value).length > 100) {
          return <span className={styles.longText}>{record.value}</span>;
        }
        // 其他字段省略显示
        return record.value;
      },
    },
  ];

  const dataSource = useMemo(() => {
    return Object.entries(data).map(([key, value], index) => ({
      key: `${key}_${index}`,
      field: key,
      value: String(value ?? ''),
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
      children: (
        <ReactJson
          src={data}
          collapsed={2}
          enableClipboard={true}
          displayDataTypes={false}
          name={false}
          style={{ fontSize: 12 }}
        />
      ),
    },
  ];

  return <Tabs className={styles.expandedRow} size="small" defaultActiveKey="table" items={items} />;
};

export default ExpandedRow;
